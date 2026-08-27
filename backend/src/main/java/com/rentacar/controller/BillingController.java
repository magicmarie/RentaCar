package com.rentacar.controller;

import com.rentacar.dto.billing.BillResponse;
import com.rentacar.entity.Role;
import com.rentacar.security.UserPrincipal;
import com.rentacar.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bills")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/reservation/{reservationId}")
    public BillResponse getForReservation(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long reservationId) {
        boolean isStaffOrAdmin = principal.getUser().getRole() != Role.CUSTOMER;
        var bill = billingService.getBillForReservation(reservationId, principal.getId(), isStaffOrAdmin);
        return BillResponse.from(bill);
    }
}
