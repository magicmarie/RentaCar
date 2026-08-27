package com.rentacar.dto.reservation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateReservationRequest(
        @NotNull(message = "is required") Long vehicleId,
        @NotNull(message = "is required") LocalDate startDate,
        @NotNull(message = "is required") LocalDate endDate
) {
}
