package com.rentacar.service;

import com.rentacar.entity.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Simple rule-based filtering over available vehicles (passenger count, budget), per
 * Vision Document §4.2: no external AI/ML service, runs in-process within the
 * Reservation subsystem.
 */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ReservationService reservationService;

    public List<Vehicle> recommend(LocalDate startDate, LocalDate endDate, Integer passengers, BigDecimal budget) {
        return reservationService.searchAvailability(startDate, endDate, null).stream()
                .filter(v -> passengers == null || v.getSeatingCapacity() >= passengers)
                .filter(v -> budget == null || v.getCategory().getDailyRate().compareTo(budget) <= 0)
                .sorted(Comparator.comparing(v -> v.getCategory().getDailyRate()))
                .toList();
    }
}
