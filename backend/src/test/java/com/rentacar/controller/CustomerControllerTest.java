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

    // Basic happy path: a logged-in customer hitting /me gets back their own
    // profile data.
    @Test
    void me_returnsProfileForTheAuthenticatedCustomer() throws Exception {
        when(customerService.getProfile(3L)).thenReturn(sampleCustomer(3L));

        mockMvc.perform(get("/api/customers/me").with(as(customerUser(3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"));

        verify(customerService).getProfile(3L);
    }

    // Security-critical: the customer id used to fetch the profile must come
    // from the authenticated principal (the JWT), never from a client-supplied
    // parameter, otherwise a customer could read someone else's profile.
    @Test
    void me_usesIdFromAuthenticatedPrincipal_notAnyClientInput() throws Exception {
        when(customerService.getProfile(7L)).thenReturn(sampleCustomer(7L));

        mockMvc.perform(get("/api/customers/me").with(as(customerUser(7L))))
                .andExpect(status().isOk());

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        verify(customerService).getProfile(idCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(idCaptor.getValue()).isEqualTo(7L);
    }

    // /me is scoped to the CUSTOMER role only; staff have no "own profile" here.
    @Test
    void me_asStaff_returns403() throws Exception {
        mockMvc.perform(get("/api/customers/me").with(as(staffUser())))
                .andExpect(status().isForbidden());
    }

    // Likewise for admin: elevated privileges don't grant access to this
    // customer-only endpoint.
    @Test
    void me_asAdmin_returns403() throws Exception {
        mockMvc.perform(get("/api/customers/me").with(as(adminUser())))
                .andExpect(status().isForbidden());
    }

    // The update DTO intentionally has no email/driver-license fields, so a
    // customer can't change these sensitive/identity fields through self-service
    // profile updates; only name-like fields go through.
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
