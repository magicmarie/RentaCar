package com.rentacar.dto.category;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CategoryRequest(
        @NotBlank(message = "is required") String name,
        @NotNull(message = "is required") @DecimalMin(value = "0.0", inclusive = false, message = "must be greater than 0") BigDecimal dailyRate
) {
}
