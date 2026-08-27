package com.rentacar.dto.vehicle;

import com.rentacar.entity.Vehicle;

import java.math.BigDecimal;

public record VehicleResponse(
        Long id,
        String make,
        String model,
        int year,
        String licensePlate,
        int seatingCapacity,
        Long categoryId,
        String categoryName,
        BigDecimal dailyRate,
        String status
) {
    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getLicensePlate(),
                vehicle.getSeatingCapacity(),
                vehicle.getCategory().getId(),
                vehicle.getCategory().getName(),
                vehicle.getCategory().getDailyRate(),
                vehicle.getStatus().name()
        );
    }
}
