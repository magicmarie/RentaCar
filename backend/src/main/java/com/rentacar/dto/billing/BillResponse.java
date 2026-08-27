package com.rentacar.dto.billing;

import com.rentacar.entity.Bill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BillResponse(
        Long id,
        Long reservationId,
        String vehicleMake,
        String vehicleModel,
        String licensePlate,
        LocalDate pickupDate,
        LocalDate returnDate,
        int rentalDays,
        BigDecimal dailyRate,
        BigDecimal totalAmount,
        LocalDateTime generatedAt
) {
    public static BillResponse from(Bill bill) {
        var reservation = bill.getReservation();
        return new BillResponse(
                bill.getId(),
                reservation.getId(),
                reservation.getVehicle().getMake(),
                reservation.getVehicle().getModel(),
                reservation.getVehicle().getLicensePlate(),
                reservation.getPickupDateTime() != null ? reservation.getPickupDateTime().toLocalDate() : reservation.getStartDate(),
                reservation.getReturnDate(),
                bill.getRentalDays(),
                bill.getDailyRate(),
                bill.getTotalAmount(),
                bill.getGeneratedAt()
        );
    }
}
