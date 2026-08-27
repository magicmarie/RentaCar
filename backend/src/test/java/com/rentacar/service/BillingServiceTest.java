package com.rentacar.service;

import com.rentacar.entity.*;
import com.rentacar.repository.BillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private BillRepository billRepository;

    @InjectMocks
    private BillingService billingService;

    @Test
    void generateBill_computesWholeDaysTimesDailyRate() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        Vehicle vehicle = Vehicle.builder().id(10L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.RENTED).build();

        Reservation reservation = Reservation.builder()
                .id(1L).vehicle(vehicle)
                .pickupDateTime(LocalDateTime.of(2026, 1, 1, 9, 0))
                .returnDate(LocalDate.of(2026, 1, 4))
                .status(ReservationStatus.COMPLETED)
                .build();

        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        Bill bill = billingService.generateBill(reservation);

        assertThat(bill.getRentalDays()).isEqualTo(3);
        assertThat(bill.getDailyRate()).isEqualByComparingTo("40.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void computeDays_roundsPartialDayUpToOneFullDay() {
        int days = billingService.computeDays(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertThat(days).isEqualTo(1);
    }
}
