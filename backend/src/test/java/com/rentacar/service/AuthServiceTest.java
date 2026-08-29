package com.rentacar.service;

import com.rentacar.dto.auth.LoginRequest;
import com.rentacar.dto.auth.RegisterCustomerRequest;
import com.rentacar.entity.PasswordResetToken;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.repository.PasswordResetTokenRepository;
import com.rentacar.repository.UserRepository;
import com.rentacar.security.JwtService;
import com.rentacar.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Enables Mockito annotations (@Mock, @InjectMocks) in this class without
// needing a full Spring context, keeping the tests fast and dependency-free.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock replaces each real collaborator with a stub whose behavior we
    // control per-test via when(...).thenReturn(...), isolating AuthService
    // from real authentication, persistence, and email sending.
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordResetTokenRepository resetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;

    private AuthService authService;

    // Runs before every test: wires the mocks into a fresh AuthService and
    // uses ReflectionTestUtils to inject the @Value-configured reset URL,
    // which wouldn't otherwise be set outside a Spring context.
    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, resetTokenRepository,
                passwordEncoder, jwtService, emailService);
        ReflectionTestUtils.setField(authService, "resetPasswordUrl", "http://localhost:5173/reset-password");
    }

    // Verifies a successful login returns a JWT plus the caller's id/role,
    // by faking a successful Spring Security authentication result.
    @Test
    void login_returnsTokenAndProfileOnSuccess() {
        User user = User.builder().id(1L).email("jane@example.com").firstName("Jane").lastName("Doe")
                .role(Role.CUSTOMER).active(true).build();
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // Stub the security manager to "succeed" and hand back our fake principal,
        // and stub the JWT service so we can assert the token flows through untouched.
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("signed-jwt");

        var response = authService.login(new LoginRequest("jane@example.com", "secret"));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    // Verifies registration is rejected (and nothing persisted) when the
    // email is already taken, preventing two accounts sharing one email.
    @Test
    void registerCustomer_rejectsDuplicateEmail() {
        var request = new RegisterCustomerRequest("Jane", "Doe", "jane@example.com", "DL-1", "janedoe", "password123");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        // Confirms the save was never attempted, not just that an exception was thrown.
        verify(userRepository, never()).save(any());
    }

    // Same guard as above, but for a duplicate username instead of email.
    @Test
    void registerCustomer_rejectsDuplicateUsername() {
        var request = new RegisterCustomerRequest("Jane", "Doe", "jane@example.com", "DL-1", "janedoe", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername("janedoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username");

        verify(userRepository, never()).save(any());
    }

    // Same guard, but for a driver's license number already on file —
    // licenses must uniquely identify one customer account.
    @Test
    void registerCustomer_rejectsDuplicateDriverLicense() {
        var request = new RegisterCustomerRequest("Jane", "Doe", "jane@example.com", "DL-1", "janedoe", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByDriverLicenseNumber("DL-1")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("license");

        verify(userRepository, never()).save(any());
    }

    // Verifies a valid registration never stores the plaintext password and
    // always defaults new accounts to the CUSTOMER role, active immediately.
    @Test
    void registerCustomer_savesEncodedPasswordAndCustomerRole() {
        var request = new RegisterCustomerRequest("Jane", "Doe", "jane@example.com", "DL-1", "janedoe", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByDriverLicenseNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        authService.registerCustomer(request);

        // ArgumentCaptor grabs the User object actually passed to save(), so we can
        // inspect fields the service set internally (hash, role) rather than just
        // checking that save() was called.
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.isActive()).isTrue();
    }

    // Verifies that requesting a reset for an email with no account is a
    // silent no-op (no token, no email) rather than leaking whether an
    // account exists for that address.
    @Test
    void requestPasswordReset_doesNothingWhenNoAccountForEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("nobody@example.com");

        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    // Verifies a valid reset request creates an unused token tied to the
    // user and emails a link containing that token.
    @Test
    void requestPasswordReset_createsTokenAndSendsEmailWhenAccountExists() {
        User user = User.builder().id(1L).email("jane@example.com").build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        authService.requestPasswordReset("jane@example.com");

        // Capture the saved token to check its fields (owner, used flag).
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(tokenCaptor.getValue().isUsed()).isFalse();

        // Capture the emailed link to confirm it points at the configured reset URL with a token param.
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("jane@example.com"), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:5173/reset-password?token=");
    }

    // Verifies an unrecognized token can't be used to reset a password.
    @Test
    void resetPassword_rejectsUnknownToken() {
        when(resetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("bad-token", "newpassword"))
                .isInstanceOf(InvalidStateException.class);
    }

    // Verifies a token can't be replayed to reset the password a second time.
    @Test
    void resetPassword_rejectsAlreadyUsedToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").used(true).expiresAt(LocalDateTime.now().plusMinutes(10))
                .user(User.builder().id(1L).build()).build();
        when(resetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("t1", "newpassword"))
                .isInstanceOf(InvalidStateException.class);
    }

    // Verifies a token past its expiry can no longer be used, even if valid otherwise.
    @Test
    void resetPassword_rejectsExpiredToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").used(false).expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(User.builder().id(1L).build()).build();
        when(resetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("t1", "newpassword"))
                .isInstanceOf(InvalidStateException.class);
    }

    // Verifies a successful reset updates the user's password hash and
    // marks the token used (so it can't be replayed), persisting both changes.
    @Test
    void resetPassword_updatesPasswordAndMarksTokenUsed() {
        User user = User.builder().id(1L).passwordHash("old-hash").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").used(false).expiresAt(LocalDateTime.now().plusMinutes(10))
                .user(user).build();
        when(resetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpassword")).thenReturn("new-hash");

        authService.resetPassword("t1", "newpassword");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(resetTokenRepository).save(token);
    }
}
