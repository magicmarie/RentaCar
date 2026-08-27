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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, userRepository, resetTokenRepository,
                passwordEncoder, jwtService, emailService);
        ReflectionTestUtils.setField(authService, "resetPasswordUrl", "http://localhost:5173/reset-password");
    }

    @Test
    void login_returnsTokenAndProfileOnSuccess() {
        User user = User.builder().id(1L).email("jane@example.com").firstName("Jane").lastName("Doe")
                .role(Role.CUSTOMER).active(true).build();
        UserPrincipal principal = new UserPrincipal(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("signed-jwt");

        var response = authService.login(new LoginRequest("jane@example.com", "secret"));

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void registerCustomer_rejectsDuplicateEmail() {
        var request = new RegisterCustomerRequest("Jane", "Doe", "jane@example.com", "DL-1", "janedoe", "password123");
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerCustomer(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");

        verify(userRepository, never()).save(any());
    }

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

    @Test
    void registerCustomer_savesEncodedPasswordAndCustomerRole() {
        var request = new RegisterCustomerRequest("Jane", "Doe", "jane@example.com", "DL-1", "janedoe", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByDriverLicenseNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        authService.registerCustomer(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void requestPasswordReset_doesNothingWhenNoAccountForEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService.requestPasswordReset("nobody@example.com");

        verify(resetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void requestPasswordReset_createsTokenAndSendsEmailWhenAccountExists() {
        User user = User.builder().id(1L).email("jane@example.com").build();
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        authService.requestPasswordReset("jane@example.com");

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(resetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(tokenCaptor.getValue().isUsed()).isFalse();

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("jane@example.com"), linkCaptor.capture());
        assertThat(linkCaptor.getValue()).startsWith("http://localhost:5173/reset-password?token=");
    }

    @Test
    void resetPassword_rejectsUnknownToken() {
        when(resetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("bad-token", "newpassword"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void resetPassword_rejectsAlreadyUsedToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").used(true).expiresAt(LocalDateTime.now().plusMinutes(10))
                .user(User.builder().id(1L).build()).build();
        when(resetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("t1", "newpassword"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void resetPassword_rejectsExpiredToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("t1").used(false).expiresAt(LocalDateTime.now().minusMinutes(1))
                .user(User.builder().id(1L).build()).build();
        when(resetTokenRepository.findByToken("t1")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.resetPassword("t1", "newpassword"))
                .isInstanceOf(InvalidStateException.class);
    }

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
