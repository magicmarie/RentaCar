package com.rentacar.controller;

import com.rentacar.dto.billing.BillResponse;
import com.rentacar.dto.reservation.ProcessReturnRequest;
import com.rentacar.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
public class CheckInController {

    private final ReservationService reservationService;

    @PostMapping("/{reservationId}")
    public BillResponse processReturn(@PathVariable Long reservationId,
                                       @Valid @RequestBody ProcessReturnRequest request) {
        var bill = reservationService.processReturn(
                reservationId, request.returnDate(), request.conditionNotes(), request.maintenanceRequired());
        return BillResponse.from(bill);
    }
}
