package com.rentacar.controller;

import com.rentacar.dto.staff.StaffAccountRequest;
import com.rentacar.dto.staff.StaffAccountResponse;
import com.rentacar.dto.staff.StaffAccountUpdateRequest;
import com.rentacar.service.StaffAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff-accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffAccountController {

    private final StaffAccountService staffAccountService;

    @GetMapping
    public List<StaffAccountResponse> list() {
        return staffAccountService.list().stream().map(StaffAccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public StaffAccountResponse getById(@PathVariable Long id) {
        return StaffAccountResponse.from(staffAccountService.getById(id));
    }

    @PostMapping
    public ResponseEntity<StaffAccountResponse> create(@Valid @RequestBody StaffAccountRequest request) {
        return ResponseEntity.ok(StaffAccountResponse.from(staffAccountService.create(request)));
    }

    @PutMapping("/{id}")
    public StaffAccountResponse update(@PathVariable Long id, @Valid @RequestBody StaffAccountUpdateRequest request) {
        return StaffAccountResponse.from(staffAccountService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public StaffAccountResponse deactivate(@PathVariable Long id) {
        return StaffAccountResponse.from(staffAccountService.deactivate(id));
    }
}
