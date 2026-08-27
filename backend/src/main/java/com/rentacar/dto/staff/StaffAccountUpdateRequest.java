package com.rentacar.dto.staff;

import jakarta.validation.constraints.NotBlank;

public record StaffAccountUpdateRequest(
        @NotBlank(message = "is required") String firstName,
        @NotBlank(message = "is required") String lastName
) {
}
