package com.rentacar.controller;

import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StaffAccountControllerTest extends AbstractControllerTest {

    private User sampleStaff() {
        return User.builder().id(2L).firstName("Front").lastName("Desk").email("staff@rentacar.com")
                .role(Role.STAFF).active(true).build();
    }

    @Test
    void list_asAdmin_returns200() throws Exception {
        when(staffAccountService.list()).thenReturn(List.of(sampleStaff()));

        mockMvc.perform(get("/api/staff-accounts").with(as(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("staff@rentacar.com"));
    }

    @Test
    void list_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/staff-accounts").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/staff-accounts").with(as(customerUser(3))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_returns200() throws Exception {
        when(staffAccountService.create(any())).thenReturn(sampleStaff());

        mockMvc.perform(post("/api/staff-accounts").with(as(adminUser()))
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Front","lastName":"Desk","email":"staff@rentacar.com","password":"password123"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_asAdmin_returns200WithInactiveStatus() throws Exception {
        User deactivated = User.builder().id(2L).firstName("Front").lastName("Desk").email("staff@rentacar.com")
                .role(Role.STAFF).active(false).build();
        when(staffAccountService.deactivate(2L)).thenReturn(deactivated);

        mockMvc.perform(post("/api/staff-accounts/2/deactivate").with(as(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
