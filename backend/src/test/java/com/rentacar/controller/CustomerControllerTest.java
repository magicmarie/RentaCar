package com.rentacar.controller;

import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerControllerTest extends AbstractControllerTest {

    private User sampleCustomer(long id) {
        return User.builder().id(id).firstName("Jane").lastName("Doe").email("jane@example.com")
                .username("janedoe").driverLicenseNumber("DL-1").role(Role.CUSTOMER).active(true).build();
    }

    @Test
    void me_returnsProfileForTheAuthenticatedCustomer() throws Exception {
        when(customerService.getProfile(3L)).thenReturn(sampleCustomer(3L));

        mockMvc.perform(get("/api/customers/me").with(as(customerUser(3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));

        verify(customerService).getProfile(3L);
    }

    @Test
    void me_usesIdFromAuthenticatedPrincipal_notAnyClientInput() throws Exception {
        when(customerService.getProfile(7L)).thenReturn(sampleCustomer(7L));

        mockMvc.perform(get("/api/customers/me").with(as(customerUser(7L))))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(customerService).getProfile(idCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(idCaptor.getValue()).isEqualTo(7L);
    }

    @Test
    void me_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/customers/me").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/customers/me").with(as(adminUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateMe_doesNotAcceptEmailOrLicenseFields() throws Exception {
        when(customerService.updateProfile(eq(3L), org.mockito.ArgumentMatchers.any())).thenReturn(sampleCustomer(3L));

        mockMvc.perform(put("/api/customers/me").with(as(customerUser(3L)))
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Janet","lastName":"Doe"}
                                """))
                .andExpect(status().isOk());
    }
}
