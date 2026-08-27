package com.rentacar.controller;

import com.rentacar.dto.dashboard.DashboardResponse;
import com.rentacar.dto.reservation.ReservationResponse;
import com.rentacar.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse get() {
        return new DashboardResponse(
                dashboardService.vehicleCountsByStatus(),
                dashboardService.activeRentals().stream().map(ReservationResponse::from).toList(),
                dashboardService.upcomingReservations().stream().map(ReservationResponse::from).toList()
        );
    }
}
