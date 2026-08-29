package com.rentacar.controller;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest extends AbstractControllerTest {

    // Happy path: an admin loading the dashboard should see the vehicle status
    // breakdown assembled from the (mocked) dashboard service.
    @Test
    void get_asAdmin_returns200WithVehicleCounts() throws Exception {
        when(dashboardService.vehicleCountsByStatus()).thenReturn(Map.of("AVAILABLE", 6L, "RENTED", 0L));
        when(dashboardService.activeRentals()).thenReturn(List.of());
        when(dashboardService.upcomingReservations()).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard").with(as(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCountsByStatus.AVAILABLE").value(6));
    }

    // The dashboard exposes business-wide metrics reserved for admins; staff
    // should not be able to view it.
    @Test
    void get_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    // Same restriction for customers: no access to admin-only dashboard data.
    @Test
    void get_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    // With no authentication supplied, the JWT filter should reject the request
    // before it reaches the (role-restricted) controller logic at all.
    @Test
    void get_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }
}
