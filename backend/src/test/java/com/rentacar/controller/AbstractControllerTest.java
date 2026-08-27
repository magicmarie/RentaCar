package com.rentacar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentacar.config.SecurityConfig;
import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import com.rentacar.security.CustomUserDetailsService;
import com.rentacar.security.JwtAuthFilter;
import com.rentacar.security.JwtService;
import com.rentacar.security.UserPrincipal;
import com.rentacar.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Shared base for controller-slice tests. Every subclass gets the full
 * {@code @RestController}/{@code @RestControllerAdvice} layer plus the real
 * {@link SecurityConfig} (so {@code @PreAuthorize} is genuinely enforced, not just
 * present), with every service mocked out. Because every subclass declares an
 * identical {@code @WebMvcTest} + {@code @Import} + {@code @MockBean} signature,
 * Spring reuses a single cached context across all of them.
 */
@WebMvcTest
@Import({SecurityConfig.class, JwtAuthFilter.class})
abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected CustomUserDetailsService userDetailsService;
    @MockBean
    protected JwtService jwtService;

    @MockBean
    protected AuthService authService;
    @MockBean
    protected VehicleService vehicleService;
    @MockBean
    protected CategoryService categoryService;
    @MockBean
    protected StaffAccountService staffAccountService;
    @MockBean
    protected CustomerService customerService;
    @MockBean
    protected ReservationService reservationService;
    @MockBean
    protected RecommendationService recommendationService;
    @MockBean
    protected BillingService billingService;
    @MockBean
    protected DashboardService dashboardService;

    protected User adminUser() {
        return User.builder().id(1L).email("admin@rentacar.com").firstName("System").lastName("Admin")
                .role(Role.ADMIN).active(true).build();
    }

    protected User staffUser() {
        return User.builder().id(2L).email("staff@rentacar.com").firstName("Front").lastName("Desk")
                .role(Role.STAFF).active(true).build();
    }

    protected User customerUser(long id) {
        return User.builder().id(id).email("customer@rentacar.com").firstName("Jane").lastName("Doe")
                .role(Role.CUSTOMER).active(true).build();
    }

    /** Injects the given user's authentication directly into the security context for one request. */
    protected RequestPostProcessor as(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }
}
