package com.rentacar.controller;

import com.rentacar.entity.*;
import com.rentacar.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckInControllerTest extends AbstractControllerTest {

    private Bill sampleBill() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
        Vehicle vehicle = Vehicle.builder().id(1L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build();
        Reservation reservation = Reservation.builder().id(1L).customer(customerUser(3)).vehicle(vehicle)
                .startDate(LocalDate.now().minusDays(3)).endDate(LocalDate.now())
                .pickupDateTime(LocalDateTime.now().minusDays(3)).returnDate(LocalDate.now())
                .status(ReservationStatus.COMPLETED).build();
        return Bill.builder().id(1L).reservation(reservation).rentalDays(3)
                .dailyRate(new BigDecimal("40.00")).totalAmount(new BigDecimal("120.00"))
                .generatedAt(LocalDateTime.now()).build();
    }

    @Test
    void processReturn_asStaff_returnsBillSummary() throws Exception {
        when(reservationService.processReturn(eq(1L), any(), any(), anyBoolean())).thenReturn(sampleBill());

        mockMvc.perform(post("/api/checkin/1").with(as(staffUser()))
                        .contentType("application/json")
                        .content("""
                                {"returnDate":"2026-09-05","conditionNotes":"fine","maintenanceRequired":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(120.00));
    }

    @Test
    void processReturn_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/checkin/1").with(as(customerUser(3)))
                        .contentType("application/json")
                        .content("""
                                {"returnDate":"2026-09-05"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void processReturn_missingReturnDate_returns400() throws Exception {
        mockMvc.perform(post("/api/checkin/1").with(as(staffUser()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void processReturn_invalidReservationState_returns422() throws Exception {
        doThrow(new InvalidStateException("Return cannot be processed for this reservation"))
                .when(reservationService).processReturn(eq(1L), any(), any(), anyBoolean());

        mockMvc.perform(post("/api/checkin/1").with(as(staffUser()))
                        .contentType("application/json")
                        .content("""
                                {"returnDate":"2026-09-05"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }
}
