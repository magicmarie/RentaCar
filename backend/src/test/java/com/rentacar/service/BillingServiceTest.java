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

// Enables Mockito annotations (@Mock, @InjectMocks) without a full Spring context.
@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    // @Mock stubs out persistence so we can control what save() returns.
    @Mock
    private BillRepository billRepository;

    // @InjectMocks builds a real BillingService and wires the @Mock fields
    // above into its constructor/fields automatically.
    @InjectMocks
    private BillingService billingService;

    // Verifies the core billing math: rental days (rounded up) times the
    // category's daily rate produces the correct total on the saved bill.
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

        // Have save() just return whatever Bill it was given, so we can assert
        // on the fields the service computed rather than mocking real persistence.
        when(billRepository.save(any(Bill.class))).thenAnswer(inv -> inv.getArgument(0));

        Bill bill = billingService.generateBill(reservation);

        assertThat(bill.getRentalDays()).isEqualTo(3);
        assertThat(bill.getDailyRate()).isEqualByComparingTo("40.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("120.00");
    }

    // Verifies a pickup and return on the same calendar date still bills as
    // one full rental day rather than zero.
    @Test
    void computeDays_roundsPartialDayUpToOneFullDay() {
        int days = billingService.computeDays(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertThat(days).isEqualTo(1);
    }
}
