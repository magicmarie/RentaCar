package com.rentacar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Boots the full Spring application context, like a real startup, so
// misconfigured beans/wiring fail the build instead of surfacing at runtime.
@SpringBootTest
// Loads application-dev.properties/yml (e.g. dev DB config) for this boot.
@ActiveProfiles("dev")
class RentaCarApplicationTests {

    // No assertions needed: if any bean fails to wire, Spring throws during
    // startup and this test fails. An empty, passing test means the app boots.
    @Test
    void contextLoads() {
    }
}
