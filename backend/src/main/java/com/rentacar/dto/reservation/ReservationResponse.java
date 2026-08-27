package com.rentacar.dto.reservation;

import com.rentacar.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long customerId,
        String customerName,
        Long vehicleId,
        String vehicleMake,
        String vehicleModel,
        String licensePlate,
        String categoryName,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime pickupDateTime,
        LocalDate returnDate,
        String conditionNotes
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCustomer().getId(),
                r.getCustomer().getFirstName() + " " + r.getCustomer().getLastName(),
                r.getVehicle().getId(),
                r.getVehicle().getMake(),
                r.getVehicle().getModel(),
                r.getVehicle().getLicensePlate(),
                r.getVehicle().getCategory().getName(),
                r.getStartDate(),
                r.getEndDate(),
                r.getStatus().name(),
                r.getPickupDateTime(),
                r.getReturnDate(),
                r.getConditionNotes()
        );
    }
}
