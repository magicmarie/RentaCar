package com.rentacar.controller;

import com.rentacar.dto.auth.LoginResponse;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.InvalidStateException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractControllerTest {

    // Verifies a successful login returns the JWT and user role the frontend
    // needs to authenticate subsequent requests and gate UI by role.
    @Test
    void login_returns200WithTokenOnSuccess() throws Exception {
        when(authService.login(any())).thenReturn(
                new LoginResponse("jwt-token", 1L, "Jane", "Doe", "jane@example.com", "CUSTOMER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"jane@example.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    // Wrong username/password should surface as 401, not a generic 500 or 200
    // with an error body, so clients can distinguish "bad login" reliably.
    @Test
    void login_returns401OnBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"jane@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // A malformed request (empty JSON body) should fail validation before it ever
    // reaches authService, confirming bean-validation annotations are wired up.
    @Test
    void login_returns400OnMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // Happy-path registration: a well-formed new-customer payload should succeed
    // and return a confirmation message body.
    @Test
    void register_returns200OnSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Jane","lastName":"Doe","email":"jane@example.com",
                                 "driverLicenseNumber":"DL-1","username":"janedoe","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // Registering with an email that's already taken should map to 409 Conflict,
    // confirming the controller translates this domain exception to the right
    // HTTP status instead of leaking a 500.
    @Test
    void register_returns409OnDuplicateEmail() throws Exception {
        doThrow(new DuplicateResourceException("An account with this email already exists"))
                .when(authService).registerCustomer(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Jane","lastName":"Doe","email":"jane@example.com",
                                 "driverLicenseNumber":"DL-1","username":"janedoe","password":"password123"}
                                """))
                .andExpect(status().isConflict());
    }

    // Forgot-password should return 200 even for an email that doesn't exist in
    // the system, so the endpoint can't be used to enumerate registered users.
    @Test
    void forgotPassword_alwaysReturns200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType("application/json")
                        .content("""
                                {"email":"nobody@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // An expired or unrecognized reset token should be rejected as 422
    // Unprocessable Entity rather than silently succeeding or 500ing.
    @Test
    void resetPassword_returns422OnInvalidToken() throws Exception {
        doThrow(new InvalidStateException("This reset link is invalid or has expired"))
                .when(authService).resetPassword(any(), any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType("application/json")
                        .content("""
                                {"token":"bad-token","newPassword":"newpassword123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
