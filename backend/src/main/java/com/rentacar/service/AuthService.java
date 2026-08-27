package com.rentacar.service;

import com.rentacar.dto.auth.LoginRequest;
import com.rentacar.dto.auth.LoginResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int RESET_TOKEN_VALID_MINUTES = 30;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Value("${rentacar.frontend.reset-password-url}")
    private String resetPasswordUrl;

    public LoginResponse login(LoginRequest request) {
        var authToken = new UsernamePasswordAuthenticationToken(request.usernameOrEmail(), request.password());
        var authentication = authenticationManager.authenticate(authToken);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        User user = principal.getUser();

        return new LoginResponse(token, user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getRole().name());
    }

    @Transactional
    public void registerCustomer(RegisterCustomerRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account with this email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("This username is already taken");
        }
        if (userRepository.existsByDriverLicenseNumber(request.driverLicenseNumber())) {
            throw new DuplicateResourceException("An account with this driver's license number already exists");
        }

        User customer = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .username(request.username())
                .driverLicenseNumber(request.driverLicenseNumber())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        userRepository.save(customer);
    }

    // Caller (AuthController) returns the same confirmation message to the client
    // regardless of whether an account exists, per SRS UC1.3 (does not reveal
    // registered emails) — so this method never reports success/failure back.
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_VALID_MINUTES))
                    .used(false)
                    .build();
            resetTokenRepository.save(resetToken);

            String resetLink = resetPasswordUrl + "?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidStateException("This reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidStateException("This reset link is invalid or has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }
}
