package com.rentacar.service;

import com.rentacar.dto.vehicle.VehicleRequest;
import com.rentacar.dto.vehicle.VehicleUpdateRequest;
import com.rentacar.entity.Category;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.exception.DuplicateResourceException;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.CategoryRepository;
import com.rentacar.repository.ReservationRepository;
import com.rentacar.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final List<ReservationStatus> ACTIVE_OR_UPCOMING =
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT);

    private final VehicleRepository vehicleRepository;
    private final CategoryRepository categoryRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public Vehicle create(VehicleRequest request) {
        if (vehicleRepository.existsByLicensePlateIgnoreCase(request.licensePlate())) {
            throw new DuplicateResourceException("A vehicle with this license plate already exists");
        }
        Category category = findCategory(request.categoryId());

        Vehicle vehicle = Vehicle.builder()
                .make(request.make())
                .model(request.model())
                .year(request.year())
                .licensePlate(request.licensePlate())
                .seatingCapacity(request.seatingCapacity())
                .category(category)
                .status(VehicleStatus.AVAILABLE)
                .build();

        return vehicleRepository.save(vehicle);
    }

    public List<Vehicle> list(Long categoryId, VehicleStatus status) {
        return vehicleRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(v -> categoryId == null || v.getCategory().getId().equals(categoryId))
                .filter(v -> status == null || v.getStatus() == status)
                .toList();
    }

    public Vehicle getById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

    @Transactional
    public Vehicle update(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = getById(id);
        Category category = findCategory(request.categoryId());

        vehicle.setMake(request.make());
        vehicle.setModel(request.model());
        vehicle.setYear(request.year());
        vehicle.setSeatingCapacity(request.seatingCapacity());
        vehicle.setCategory(category);

        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = getById(id);

        if (reservationRepository.existsByVehicleIdAndStatusIn(id, ACTIVE_OR_UPCOMING)) {
            throw new InvalidStateException("This vehicle cannot be deleted because it is associated with a reservation");
        }

        vehicleRepository.delete(vehicle);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }
}
