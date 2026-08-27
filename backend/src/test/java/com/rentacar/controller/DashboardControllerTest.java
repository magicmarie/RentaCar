package com.rentacar.controller;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest extends AbstractControllerTest {

    @Test
    void get_asAdmin_returns200WithVehicleCounts() throws Exception {
        when(dashboardService.vehicleCountsByStatus()).thenReturn(Map.of("AVAILABLE", 6L, "RENTED", 0L));
        when(dashboardService.activeRentals()).thenReturn(List.of());
        when(dashboardService.upcomingReservations()).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard").with(as(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCountsByStatus.AVAILABLE").value(6));
    }

    @Test
    void get_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
