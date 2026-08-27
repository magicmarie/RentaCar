package com.rentacar.dto.vehicle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VehicleRequest(
        @NotBlank(message = "is required") String make,
        @NotBlank(message = "is required") String model,
        @Min(value = 1980, message = "must be a valid year") int year,
        @NotBlank(message = "is required") String licensePlate,
        @Min(value = 1, message = "must be at least 1") int seatingCapacity,
        @NotNull(message = "is required") Long categoryId
) {
}
