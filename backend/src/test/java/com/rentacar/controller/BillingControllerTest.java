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

    @Test
    void getForReservation_passesIsStaffOrAdminFalse_forCustomer() throws Exception {
        when(billingService.getBillForReservation(eq(1L), any(), any(Boolean.class))).thenReturn(sampleBill());

        mockMvc.perform(get("/api/bills/reservation/1").with(as(customerUser(3))))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> flag = ArgumentCaptor.forClass(Boolean.class);
        verify(billingService).getBillForReservation(eq(1L), eq(3L), flag.capture());
        assertThat(flag.getValue()).isFalse();
    }

    @Test
    void getForReservation_passesIsStaffOrAdminTrue_forStaff() throws Exception {
        when(billingService.getBillForReservation(eq(1L), any(), any(Boolean.class))).thenReturn(sampleBill());

        mockMvc.perform(get("/api/bills/reservation/1").with(as(staffUser())))
                .andExpect(status().isOk());

        ArgumentCaptor<Boolean> flag = ArgumentCaptor.forClass(Boolean.class);
        verify(billingService).getBillForReservation(eq(1L), eq(2L), flag.capture());
        assertThat(flag.getValue()).isTrue();
    }

    @Test
    void getForReservation_serviceRejectsAccessToSomeoneElsesBill_returns403() throws Exception {
        when(billingService.getBillForReservation(eq(1L), any(), any(Boolean.class)))
                .thenThrow(new AccessDeniedException("Not your bill"));

        mockMvc.perform(get("/api/bills/reservation/1").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }
}
