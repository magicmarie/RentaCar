package com.rentacar.controller;

import com.rentacar.dto.customer.CustomerProfileResponse;
import com.rentacar.dto.customer.CustomerProfileUpdateRequest;
import com.rentacar.security.UserPrincipal;
import com.rentacar.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public CustomerProfileResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return CustomerProfileResponse.from(customerService.getProfile(principal.getId()));
    }

    @PutMapping("/me")
    public CustomerProfileResponse updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                             @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return CustomerProfileResponse.from(customerService.updateProfile(principal.getId(), request));
    }
}
