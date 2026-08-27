package com.rentacar.controller;

import com.rentacar.dto.reservation.CreateReservationRequest;
import com.rentacar.dto.reservation.ReservationResponse;
import com.rentacar.dto.vehicle.VehicleResponse;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.Role;
import com.rentacar.security.UserPrincipal;
import com.rentacar.service.RecommendationService;
import com.rentacar.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final RecommendationService recommendationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public List<ReservationResponse> search(@RequestParam(required = false) ReservationStatus status,
                                             @RequestParam(required = false) String query) {
        return reservationService.search(status, query).stream().map(ReservationResponse::from).toList();
    }

    @GetMapping("/available")
    public List<VehicleResponse> searchAvailability(@RequestParam LocalDate startDate,
                                                      @RequestParam LocalDate endDate,
                                                      @RequestParam(required = false) Long categoryId) {
        return reservationService.searchAvailability(startDate, endDate, categoryId).stream()
                .map(VehicleResponse::from).toList();
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<VehicleResponse> recommend(@RequestParam LocalDate startDate,
                                            @RequestParam LocalDate endDate,
                                            @RequestParam(required = false) Integer passengers,
                                            @RequestParam(required = false) BigDecimal budget) {
        return recommendationService.recommend(startDate, endDate, passengers, budget).stream()
                .map(VehicleResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                        @Valid @RequestBody CreateReservationRequest request) {
        var reservation = reservationService.createReservation(
                principal.getId(), request.vehicleId(), request.startDate(), request.endDate());
        return ResponseEntity.ok(ReservationResponse.from(reservation));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ReservationResponse> myHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return reservationService.getHistory(principal.getId()).stream().map(ReservationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ReservationResponse getById(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        boolean isStaffOrAdmin = principal.getUser().getRole() != Role.CUSTOMER;
        return ReservationResponse.from(reservationService.getById(id, principal.getId(), isStaffOrAdmin));
    }

    @PostMapping("/{id}/cancel")
    public ReservationResponse cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        boolean isStaffOrAdmin = principal.getUser().getRole() != Role.CUSTOMER;
        return ReservationResponse.from(reservationService.cancelReservation(id, principal.getId(), isStaffOrAdmin));
    }
}
