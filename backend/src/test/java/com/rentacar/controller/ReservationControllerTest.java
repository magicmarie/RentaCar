package com.rentacar.controller;

import com.rentacar.entity.Category;
import com.rentacar.entity.Reservation;
import com.rentacar.entity.ReservationStatus;
import com.rentacar.entity.User;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationControllerTest extends AbstractControllerTest {

    private Reservation sampleReservation() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        Vehicle vehicle = Vehicle.builder().id(10L).make("Toyota").model("Corolla")
                .licensePlate("ECO-101").category(economy).status(VehicleStatus.AVAILABLE).build();
        User customer = customerUser(3);
        return Reservation.builder().id(1L).customer(customer).vehicle(vehicle)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.PENDING).build();
    }

    @Test
    void search_asStaff_returns200() throws Exception {
        when(reservationService.search(isNull(), isNull())).thenReturn(List.of(sampleReservation()));

        mockMvc.perform(get("/api/reservations").with(as(staffUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void search_asAdmin_returns200() throws Exception {
        when(reservationService.search(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/reservations").with(as(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void search_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/reservations").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    @Test
    void search_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_passesStatusAndQueryThrough() throws Exception {
        when(reservationService.search(ReservationStatus.PENDING, "doe")).thenReturn(List.of(sampleReservation()));

        mockMvc.perform(get("/api/reservations").with(as(staffUser()))
                        .param("status", "PENDING")
                        .param("query", "doe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
