package com.rentacar.dto.dashboard;

import com.rentacar.dto.reservation.ReservationResponse;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        Map<String, Long> vehicleCountsByStatus,
        List<ReservationResponse> activeRentals,
        List<ReservationResponse> upcomingReservations
) {
}
