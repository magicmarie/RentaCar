package com.rentacar.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
        @NotBlank(message = "is required") String firstName,
        @NotBlank(message = "is required") String lastName,
        @NotBlank(message = "is required") @Email(message = "must be a valid email") String email,
        @NotBlank(message = "is required") String driverLicenseNumber,
        @NotBlank(message = "is required") String username,
        @NotBlank(message = "is required") @Size(min = 6, message = "must be at least 6 characters") String password
) {
}
