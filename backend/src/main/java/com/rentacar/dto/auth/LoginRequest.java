package com.rentacar.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "is required") String usernameOrEmail,
        @NotBlank(message = "is required") String password
) {
}
