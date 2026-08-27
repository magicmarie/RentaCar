package com.rentacar.repository;

import com.rentacar.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByLicensePlateIgnoreCase(String licensePlate);

    List<Vehicle> findByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);
}
