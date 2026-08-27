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

    @Test
    void list_asAdmin_returns200() throws Exception {
        when(vehicleService.list(null, null)).thenReturn(List.of(sampleVehicle()));

        mockMvc.perform(get("/api/vehicles").with(as(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].licensePlate").value("ECO-101"));
    }

    @Test
    void list_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/vehicles").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());
    }

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

    @Test
    void create_withMissingRequiredField_returns400() throws Exception {
        mockMvc.perform(post("/api/vehicles").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"model":"Corolla","year":2022,"licensePlate":"ECO-101","seatingCapacity":5,"categoryId":1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_blockedByActiveReservation_returns422WithMessage() throws Exception {
        doThrow(new InvalidStateException("This vehicle cannot be deleted because it is associated with a reservation"))
                .when(vehicleService).delete(1L);

        mockMvc.perform(delete("/api/vehicles/1").with(as(adminUser())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This vehicle cannot be deleted because it is associated with a reservation"));
    }

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
