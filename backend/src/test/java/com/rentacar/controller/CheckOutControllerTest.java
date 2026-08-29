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

    // A reservation already marked CHECKED_OUT with its vehicle now RENTED,
    // representing the state after a successful check-out call.
    private Reservation sampleReservation() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(BigDecimal.TEN).build();
        Vehicle vehicle = Vehicle.builder().id(1L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.RENTED).build();
        User customer = customerUser(3);
        return Reservation.builder().id(1L).customer(customer).vehicle(vehicle)
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(2))
                .status(ReservationStatus.CHECKED_OUT).build();
    }

    // Happy path: staff handing over a vehicle to a customer with an explicit
    // pickup time should succeed.
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

    // The pickup time body is optional (e.g. the service can default it), so
    // omitting the request body entirely should not fail validation.
    @Test
    void checkOut_withoutBody_stillWorks() throws Exception {
        when(reservationService.checkOut(eq(1L), any())).thenReturn(sampleReservation());

        mockMvc.perform(post("/api/checkout/1").with(as(staffUser())))
                .andExpect(status().isOk());
    }

    // Check-out is a front-desk (staff) operation; even an admin isn't granted
    // this permission, confirming the access rule is role-specific, not
    // "any elevated role."
    @Test
    void checkOut_asAdmin_returns403() throws Exception {
        mockMvc.perform(post("/api/checkout/1").with(as(adminUser())))
                .andExpect(status().isForbidden());
    }

    // Customers cannot check themselves out; only staff perform this action.
    @Test
    void checkOut_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/checkout/1").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    // e.g. checking out a reservation that's already been checked out or was
    // cancelled; the service's InvalidStateException must map to 422.
    @Test
    void checkOut_invalidReservationState_returns422() throws Exception {
        doThrow(new InvalidStateException("Check-out cannot proceed for this reservation"))
                .when(reservationService).checkOut(eq(1L), any());

        mockMvc.perform(post("/api/checkout/1").with(as(staffUser())))
                .andExpect(status().isUnprocessableEntity());
    }
}
