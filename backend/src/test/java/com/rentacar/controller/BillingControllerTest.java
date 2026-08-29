package com.rentacar.controller;

import com.rentacar.entity.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BillingControllerTest extends AbstractControllerTest {

    // Builds a minimal but fully-linked Bill -> Reservation -> Vehicle -> Category
    // graph, since the controller/serializer touch fields across all of them.
    private Bill sampleBill() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        Vehicle vehicle = Vehicle.builder().id(1L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build();
        Reservation reservation = Reservation.builder().id(1L).customer(customerUser(3)).vehicle(vehicle)
                .pickupDateTime(LocalDateTime.now().minusDays(3)).returnDate(LocalDate.now())
                .status(ReservationStatus.COMPLETED).build();
        return Bill.builder().id(1L).reservation(reservation).rentalDays(3)
                .dailyRate(new BigDecimal("40.00")).totalAmount(new BigDecimal("120.00"))
                .generatedAt(LocalDateTime.now()).build();
    }

    // The controller must tell the service whether the caller is staff/admin so it
    // can decide whether to enforce "only the owning customer may view this bill".
    // For a customer caller, that flag must be false, otherwise the ownership
    // check the service performs would be silently skipped.
    @Test
    void getForReservation_passesIsStaffOrAdminFalse_forCustomer() throws Exception {
        when(billingService.getBillForReservation(eq(1L), any(), any(Boolean.class))).thenReturn(sampleBill());

        mockMvc.perform(get("/api/bills/reservation/1").with(as(customerUser(3))))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> flag = ArgumentCaptor.forClass(Boolean.class);
        verify(billingService).getBillForReservation(eq(1L), eq(3L), flag.capture());
        assertThat(flag.getValue()).isFalse();
    }

    // Mirror of the test above: staff callers should be flagged as
    // staff-or-admin so they can view bills that aren't their own.
    @Test
    void getForReservation_passesIsStaffOrAdminTrue_forStaff() throws Exception {
        when(billingService.getBillForReservation(eq(1L), any(), any(Boolean.class))).thenReturn(sampleBill());

        mockMvc.perform(get("/api/bills/reservation/1").with(as(staffUser())))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> flag = ArgumentCaptor.forClass(Boolean.class);
        verify(billingService).getBillForReservation(eq(1L), eq(2L), flag.capture());
        assertThat(flag.getValue()).isTrue();
    }

    // If the service throws AccessDeniedException (e.g. a customer requesting
    // someone else's bill), the controller must surface it as 403, not 500.
    @Test
    void getForReservation_serviceRejectsAccessToSomeoneElsesBill_returns403() throws Exception {
        when(billingService.getBillForReservation(eq(1L), any(), any(Boolean.class)))
                .thenThrow(new AccessDeniedException("Not your bill"));

        mockMvc.perform(get("/api/bills/reservation/1").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }
}
