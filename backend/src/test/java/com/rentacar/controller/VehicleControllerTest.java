package com.rentacar.controller;

import com.rentacar.entity.Category;
import com.rentacar.entity.Vehicle;
import com.rentacar.entity.VehicleStatus;
import com.rentacar.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleControllerTest extends AbstractControllerTest {

    private Vehicle sampleVehicle() {
        Category economy = Category.builder().id(1L).name("Economy").dailyRate(java.math.BigDecimal.TEN).build();
        return Vehicle.builder().id(1L).make("Toyota").model("Corolla").year(2022)
                .licensePlate("ECO-101").seatingCapacity(5).category(economy).status(VehicleStatus.AVAILABLE).build();
    }

    // Happy path: an admin listing vehicles (with no filters) gets back the
    // mocked fleet data.
    @Test
    void list_asAdmin_returns200() throws Exception {
        when(vehicleService.list(null, null)).thenReturn(List.of(sampleVehicle()));

        mockMvc.perform(get("/api/vehicles").with(as(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("ECO-101"));
    }

    // This particular listing endpoint is restricted to admins; staff must be
    // denied even though they need vehicle data for other operations.
    @Test
    void list_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    // Customers also can't reach this admin listing endpoint.
    @Test
    void list_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    // Unauthenticated calls should be stopped by the JWT filter before
    // authorization is even checked.
    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    // Happy path: an admin submitting a complete, valid vehicle payload should
    // have it created and returned with its initial AVAILABLE status.
    @Test
    void create_asAdmin_returns200() throws Exception {
        when(vehicleService.create(any())).thenReturn(sampleVehicle());

        mockMvc.perform(post("/api/vehicles").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"make":"Toyota","model":"Corolla","year":2022,
                                 "licensePlate":"ECO-101","seatingCapacity":5,"categoryId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    // "make" is omitted from the payload; bean validation should catch this
    // and return 400 before the request reaches vehicleService.
    @Test
    void create_withMissingRequiredField_returns400() throws Exception {
        mockMvc.perform(post("/api/vehicles").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"model":"Corolla","year":2022,"licensePlate":"ECO-101","seatingCapacity":5,"categoryId":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    // A vehicle tied to an existing reservation can't be deleted without
    // orphaning that reservation; verifies both the 422 status and that the
    // service's explanatory message is passed through to the client.
    @Test
    void delete_blockedByActiveReservation_returns422WithMessage() throws Exception {
        doThrow(new InvalidStateException("This vehicle cannot be deleted because it is associated with a reservation"))
                .when(vehicleService).delete(1L);

        mockMvc.perform(delete("/api/vehicles/1").with(as(adminUser())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This vehicle cannot be deleted because it is associated with a reservation"));
    }

    // The update DTO has no licensePlate field, so it can't be changed via a
    // regular update (it's treated as immutable identifying data); the update
    // should still succeed using only the fields that are allowed.
    @Test
    void update_doesNotAcceptLicensePlateField() throws Exception {
        when(vehicleService.update(eq(1L), any())).thenReturn(sampleVehicle());

        mockMvc.perform(put("/api/vehicles/1").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"make":"Toyota","model":"Corolla","year":2022,"seatingCapacity":5,"categoryId":1}
                                """))
                .andExpect(status().isOk());
    }
}
