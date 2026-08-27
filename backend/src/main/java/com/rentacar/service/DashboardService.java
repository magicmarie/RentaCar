package com.rentacar.service;

import com.rentacar.entity.Reservation;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.repository.ReservationRepository;
import com.rentacar.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final ReservationRepository reservationRepository;

    public Map<String, Long> vehicleCountsByStatus() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (VehicleStatus status : VehicleStatus.values()) {
            counts.put(status.name(), vehicles.stream().filter(v -> v.getStatus() == status).count());
        }
        return counts;
    }

    public List<Reservation> activeRentals() {
        return reservationRepository.findByStatus(ReservationStatus.CHECKED_OUT);
    }

    public List<Reservation> upcomingReservations() {
        return reservationRepository.findUpcoming(
                List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED), LocalDate.now());
    }
}
