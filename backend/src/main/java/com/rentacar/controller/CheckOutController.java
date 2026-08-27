package com.rentacar.controller;

import com.rentacar.dto.reservation.CheckOutRequest;
import com.rentacar.dto.reservation.ReservationResponse;
import com.rentacar.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
public class CheckOutController {

    private final ReservationService reservationService;

    @PostMapping("/{reservationId}")
    public ReservationResponse checkOut(@PathVariable Long reservationId,
                                         @RequestBody(required = false) CheckOutRequest request) {
        var pickupDateTime = request != null ? request.pickupDateTime() : null;
        return ReservationResponse.from(reservationService.checkOut(reservationId, pickupDateTime));
    }
}
