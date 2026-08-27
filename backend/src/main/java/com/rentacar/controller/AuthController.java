package com.rentacar.controller;

import com.rentacar.dto.auth.ForgotPasswordRequest;
import com.rentacar.dto.auth.LoginRequest;
import com.rentacar.dto.auth.LoginResponse;
import com.rentacar.dto.auth.RegisterCustomerRequest;
import com.rentacar.dto.auth.ResetPasswordRequest;
import com.rentacar.dto.common.MessageResponse;
import com.rentacar.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String RESET_CONFIRMATION_MESSAGE =
            "If an account exists for that email, a password reset link has been sent";

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        authService.registerCustomer(request);
        return ResponseEntity.ok(new MessageResponse("Account created successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(new MessageResponse(RESET_CONFIRMATION_MESSAGE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Password updated successfully"));
    }
}
