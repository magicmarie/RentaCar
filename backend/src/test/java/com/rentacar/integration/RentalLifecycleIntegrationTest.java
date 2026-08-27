package com.rentacar.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack tests against the real Spring context (real H2 DB seeded by
 * {@link com.rentacar.config.DataSeeder}, real Spring Security filter chain, real
 * JWT issuance/validation) via MockMvc, exercising exactly the rental lifecycle and
 * cross-cutting rules verified manually during development: reservation -> check-out
 * -> check-in -> bill, double-booking prevention, and RBAC at the HTTP boundary.
 * {@code @Transactional} rolls each test's changes back afterward so tests don't
 * interfere with each other or with the DataSeeder-seeded baseline data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class RentalLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String loginAndGetToken(String usernameOrEmail, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new LoginPayload(usernameOrEmail, password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    @Test
    void fullRentalLifecycle_fromReservationThroughBill() throws Exception {
        String customerToken = loginAndGetToken("customer@rentacar.com", "customer123");
        String staffToken = loginAndGetToken("staff@rentacar.com", "staff123");
        String adminToken = loginAndGetToken("admin@rentacar.com", "admin123");

        // 1. Search availability
        mockMvc.perform(get("/api/reservations/available")
                        .param("startDate", "2027-01-10").param("endDate", "2027-01-13")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.licensePlate == 'ECO-101')]").exists());

        // 2. Create a reservation
        MvcResult createResult = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType("application/json")
                        .content("""
                                {"vehicleId":1,"startDate":"2027-01-10","endDate":"2027-01-13"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long reservationId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Staff checks the vehicle out
        mockMvc.perform(post("/api/checkout/" + reservationId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType("application/json")
                        .content("""
                                {"pickupDateTime":"2027-01-10T09:00:00"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHECKED_OUT"));

        // 4. Staff checks the vehicle back in - bill is generated automatically
        MvcResult checkInResult = mockMvc.perform(post("/api/checkin/" + reservationId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType("application/json")
                        .content("""
                                {"returnDate":"2027-01-13","conditionNotes":"all good","maintenanceRequired":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalDays").value(3))
                .andExpect(jsonPath("$.totalAmount").value(120.00))
                .andReturn();
        JsonNode bill = objectMapper.readTree(checkInResult.getResponse().getContentAsString());
        assertThat(bill.get("dailyRate").asDouble()).isEqualTo(40.00);

        // 5. Customer can view their own bill; the same bill is visible to staff/admin too
        mockMvc.perform(get("/api/bills/reservation/" + reservationId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(120.00));

        mockMvc.perform(get("/api/bills/reservation/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 6. Admin dashboard reflects the completed rental
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void doubleBooking_isRejectedAtTheHttpBoundary() throws Exception {
        String token = loginAndGetToken("customer@rentacar.com", "customer123");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"vehicleId":2,"startDate":"2027-02-01","endDate":"2027-02-05"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"vehicleId":2,"startDate":"2027-02-03","endDate":"2027-02-07"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This vehicle is no longer available for those dates"));
    }

    @Test
    void customerCannotReachAdminOrStaffEndpoints() throws Exception {
        String customerToken = loginAndGetToken("customer@rentacar.com", "customer123");

        mockMvc.perform(get("/api/vehicles").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/checkout/1").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithNoToken_isRejectedWith401NotJustForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivatedStaffAccount_cannotLogIn() throws Exception {
        String adminToken = loginAndGetToken("admin@rentacar.com", "admin123");

        MvcResult createResult = mockMvc.perform(post("/api/staff-accounts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("""
                                {"firstName":"Temp","lastName":"Worker","email":"temp-worker@rentacar.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        long staffId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"temp-worker@rentacar.com","password":"password123"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/staff-accounts/" + staffId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"usernameOrEmail":"temp-worker@rentacar.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private record LoginPayload(String usernameOrEmail, String password) {
    }
}
