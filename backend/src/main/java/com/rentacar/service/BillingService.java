package com.rentacar.service;

import com.rentacar.entity.Bill;
import com.rentacar.entity.Reservation;
import com.rentacar.exception.ResourceNotFoundException;
import com.rentacar.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * «control» BillingService (UC9.1) — only ever invoked from
 * {@link ReservationService#processReturn}, never exposed on its own write endpoint,
 * per the SRS UC8-includes-UC9 relationship.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository billRepository;

    @Transactional
    public Bill generateBill(Reservation reservation) {
        LocalDate pickupDate = reservation.getPickupDateTime().toLocalDate();
        LocalDate returnDate = reservation.getReturnDate();
        int rentalDays = computeDays(pickupDate, returnDate);
        BigDecimal dailyRate = reservation.getVehicle().getCategory().getDailyRate();
        BigDecimal totalAmount = dailyRate.multiply(BigDecimal.valueOf(rentalDays));

        Bill bill = Bill.builder()
                .reservation(reservation)
                .rentalDays(rentalDays)
                .dailyRate(dailyRate)
                .totalAmount(totalAmount)
                .generatedAt(LocalDateTime.now())
                .build();

        return billRepository.save(bill);
    }

    public Bill getBillForReservation(Long reservationId, Long requestingUserId, boolean isStaffOrAdmin) {
        Bill bill = billRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("No bill found for this reservation"));

        if (!isStaffOrAdmin && !bill.getReservation().getCustomer().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("Not your bill");
        }
        return bill;
    }

    /** Whole days between pickup and return; a partial day counts as a full day. */
    int computeDays(LocalDate pickupDate, LocalDate returnDate) {
        long days = ChronoUnit.DAYS.between(pickupDate, returnDate);
        return (int) Math.max(days, 1);
    }
}
