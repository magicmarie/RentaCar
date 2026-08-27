package com.rentacar.controller;

import com.rentacar.entity.*;
import com.rentacar.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckOutControllerTest extends AbstractControllerTest {

    private Reservation sampleReservation() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(BigDecimal.TEN).build();
        Vehicle vehicle = Vehicle.builder().id(1L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.RENTED).build();
        User customer = customerUser(3);
        return Reservation.builder().id(1L).customer(customer).vehicle(vehicle)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.CHECKED_OUT).build();
    }

    @Test
    void checkOut_asStaff_returns200() throws Exception {
        when(reservationService.checkOut(eq(1L), any())).thenReturn(sampleReservation());

        mockMvc.perform(post("/api/checkout/1").with(as(staffUser()))
                        .contentType("application/json")
                        .content("""
                                {"pickupDateTime":"2026-09-01T10:00:00"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void checkOut_withoutBody_stillWorks() throws Exception {
        when(reservationService.checkOut(eq(1L), any())).thenReturn(sampleReservation());

        mockMvc.perform(post("/api/checkout/1").with(as(staffUser())))
                .andExpect(status().isOk());
    }

    @Test
    void checkOut_asAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/checkout/1").with(as(adminUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkOut_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/checkout/1").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkOut_invalidReservationState_returns422() throws Exception {
        doThrow(new InvalidStateException("Check-out cannot proceed for this reservation"))
                .when(reservationService).checkOut(eq(1L), any());

        mockMvc.perform(post("/api/checkout/1").with(as(staffUser())))
                .andExpect(status().isUnprocessableEntity());
    }
}
