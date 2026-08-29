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
// @WebMvcTest boots only the web layer (controllers, filters, exception handlers)
// for this slice, not the full app context, so tests start fast and stay focused
// on HTTP behavior rather than service/repository wiring.
@WebMvcTest
// Pulls the real security config and JWT filter into the slice context so
// role-based access rules are actually exercised, not bypassed by the test setup.
@Import({SecurityConfig.class, JwtAuthFilter.class})
abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    // @MockBean replaces the real bean in the context with a Mockito mock, so the
    // security filter chain can run without needing a real user store or JWT signing.
    @MockBean
    protected CustomUserDetailsService userDetailsService;
    @MockBean
    protected JwtService jwtService;

    // Every service the controllers depend on is mocked here so each subclass
    // only has to stub the calls relevant to the endpoint it's testing.
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

    // Fixture helpers below build minimal User instances for each role, used
    // together with as() to simulate "who is making this request" per test.
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
