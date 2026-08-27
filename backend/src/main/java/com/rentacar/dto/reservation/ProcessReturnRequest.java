package com.rentacar.dto.reservation;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProcessReturnRequest(
        @NotNull(message = "is required") LocalDate returnDate,
        String conditionNotes,
        boolean maintenanceRequired
) {
}
