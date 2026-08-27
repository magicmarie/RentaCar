package com.rentacar.dto.customer;

import jakarta.validation.constraints.NotBlank;

public record CustomerProfileUpdateRequest(
        @NotBlank(message = "is required") String firstName,
        @NotBlank(message = "is required") String lastName
) {
}
