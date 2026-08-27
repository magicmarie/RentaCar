package com.rentacar.service;

import com.rentacar.entity.Bill;
import com.rentacar.entity.Reservation;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.User;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.exception.InvalidStateException;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.ReservationRepository;
import com.rentacar.repository.UserRepository;
import com.rentacar.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    /** Statuses that block a vehicle from being booked again for overlapping dates. */
    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_OUT);

    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final BillingService billingService;

    public List<Vehicle> searchAvailability(LocalDate startDate, LocalDate endDate, Long categoryId) {
        validateDateRange(startDate, endDate);

        return vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE)
                .filter(v -> categoryId == null || v.getCategory().getId().equals(categoryId))
                .filter(v -> reservationRepository.findOverlapping(v.getId(), startDate, endDate, BLOCKING_STATUSES).isEmpty())
                .toList();
    }

    @Transactional
    public Reservation createReservation(Long customerId, Long vehicleId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        boolean overlapping = !reservationRepository.findOverlapping(vehicleId, startDate, endDate, BLOCKING_STATUSES).isEmpty();
        if (overlapping) {
            throw new InvalidStateException("This vehicle is no longer available for those dates");
        }

        Reservation reservation = Reservation.builder()
                .customer(customer)
                .vehicle(vehicle)
                .startDate(startDate)
                .endDate(endDate)
                .status(ReservationStatus.PENDING)
                .build();

        return reservationRepository.save(reservation);
    }

    public List<Reservation> getHistory(Long customerId) {
        return reservationRepository.findByCustomerIdOrderByStartDateDesc(customerId);
    }

    public List<Reservation> search(ReservationStatus status, String query) {
        String search = (query == null || query.isBlank()) ? null : "%" + query.trim().toLowerCase() + "%";
        return reservationRepository.search(status, search);
    }

    public Reservation getById(Long reservationId, Long requestingUserId, boolean isStaffOrAdmin) {
        Reservation reservation = findReservation(reservationId);
        if (!isStaffOrAdmin && !reservation.getCustomer().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Not your reservation");
        }
        return reservation;
    }

    @Transactional
    public Reservation cancelReservation(Long reservationId, Long requestingUserId, boolean isStaffOrAdmin) {
        Reservation reservation = findReservation(reservationId);

        if (!isStaffOrAdmin && !reservation.getCustomer().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Not your reservation");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new InvalidStateException("This reservation cannot be cancelled because the vehicle has already been checked out");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation checkOut(Long reservationId, LocalDateTime pickupDateTime) {
        Reservation reservation = findReservation(reservationId);
        Vehicle vehicle = reservation.getVehicle();

        boolean validReservationState = reservation.getStatus() == ReservationStatus.PENDING
                || reservation.getStatus() == ReservationStatus.CONFIRMED;
        if (!validReservationState || vehicle.getStatus() == VehicleStatus.RENTED) {
            throw new InvalidStateException("Check-out cannot proceed for this reservation");
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        reservation.setPickupDateTime(pickupDateTime != null ? pickupDateTime : LocalDateTime.now());
        vehicle.setStatus(VehicleStatus.RENTED);

        vehicleRepository.save(vehicle);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Bill processReturn(Long reservationId, LocalDate returnDate, String conditionNotes, boolean maintenanceRequired) {
        Reservation reservation = findReservation(reservationId);

        if (reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
            throw new InvalidStateException("Return cannot be processed for this reservation");
        }

        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setReturnDate(returnDate);
        reservation.setConditionNotes(conditionNotes);

        Vehicle vehicle = reservation.getVehicle();
        vehicle.setStatus(maintenanceRequired ? VehicleStatus.UNDER_MAINTENANCE : VehicleStatus.AVAILABLE);
        vehicleRepository.save(vehicle);
        reservationRepository.save(reservation);

        // <<include>> UC9.1 Calculate and Generate Bill — only ever triggered from here.
        return billingService.generateBill(reservation);
    }

    private Reservation findReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new InvalidStateException("End date must be after start date");
        }
    }
}
