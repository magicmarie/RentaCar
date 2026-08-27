package com.rentacar.controller;

import com.rentacar.entity.Category;
import com.rentacar.exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerTest extends AbstractControllerTest {

    private Category sampleCategory() {
        return Category.builder().id(1L).name("Economy").dailyRate(new BigDecimal("40.00")).build();
    }

    @Test
    void list_isReadableByAnyAuthenticatedRole() throws Exception {
        when(categoryService.list()).thenReturn(List.of(sampleCategory()));

        mockMvc.perform(get("/api/categories").with(as(customerUser(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Economy"));

        mockMvc.perform(get("/api/categories").with(as(staffUser())))
                .andExpect(status().isOk());
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_asCustomer_returns403() throws Exception {
        mockMvc.perform(post("/api/categories").with(as(customerUser(3)))
                        .contentType("application/json")
                        .content("""
                                {"name":"Van","dailyRate":55.00}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_returns200() throws Exception {
        when(categoryService.create(any())).thenReturn(sampleCategory());

        mockMvc.perform(post("/api/categories").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"name":"Economy","dailyRate":40.00}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void updateRate_rejectsNonPositiveRate() throws Exception {
        mockMvc.perform(put("/api/categories/1/rate").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"dailyRate":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_blockedByAssignedVehicles_returns422() throws Exception {
        doThrow(new InvalidStateException("This category is still assigned to some vehicles"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1").with(as(adminUser())))
                .andExpect(status().isUnprocessableEntity());
    }
}
