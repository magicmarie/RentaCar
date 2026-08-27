package com.rentacar.controller;

import com.rentacar.dto.common.MessageResponse;
import com.rentacar.dto.vehicle.VehicleRequest;
import com.rentacar.dto.vehicle.VehicleResponse;
import com.rentacar.dto.vehicle.VehicleUpdateRequest;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class VehicleController {

    private final VehicleService vehicleService;

    @GetMapping
    public List<VehicleResponse> list(@RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) VehicleStatus status) {
        return vehicleService.list(categoryId, status).stream().map(VehicleResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VehicleResponse getById(@PathVariable Long id) {
        return VehicleResponse.from(vehicleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.ok(VehicleResponse.from(vehicleService.create(request)));
    }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleUpdateRequest request) {
        return VehicleResponse.from(vehicleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return new MessageResponse("Vehicle deleted successfully");
    }
}
