package com.rentacar.security;

import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Plain unit test (no @SpringBootTest/@DataJpaTest) - JwtService has no Spring
// dependencies of its own, so it's constructed directly with `new`, keeping these
// tests fast and independent of the application context.
class JwtServiceTest {

    // A fixed HMAC signing secret used only in this test - long enough to satisfy
    // the algorithm's minimum key-length requirement.
    private static final String SECRET = "test-only-secret-key-at-least-32-bytes-long!!";

    private JwtService jwtService;
    private UserPrincipal principal;

    // Builds a JwtService with a 1-hour token lifetime and a Spring Security
    // UserPrincipal wrapping a sample customer, reused by every test below.
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600_000L);
        User user = User.builder().id(1L).email("jane@example.com").role(Role.CUSTOMER).active(true).build();
        principal = new UserPrincipal(user);
    }

    // Protects the token's claims: the subject embedded in the token must round-trip
    // back to the user's email, otherwise the server couldn't identify who a token belongs to.
    @Test
    void generateToken_producesTokenWithCorrectSubject() {
        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo("jane@example.com");
    }

    // Protects the basic validation path: a token that was just issued, with no
    // tampering, must be accepted - the baseline "happy path" that all the negative tests below contrast with.
    @Test
    void isTokenValid_trueForFreshlyIssuedToken() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    // Protects the expiry check: a token issued by a service configured with a
    // negative (already-elapsed) lifetime must be rejected. If expiry weren't enforced,
    // stolen or old tokens would remain usable forever.
    @Test
    void isTokenValid_falseForExpiredToken() {
        JwtService shortLivedService = new JwtService(SECRET, -1000L);
        String token = shortLivedService.generateToken(principal);

        assertThat(shortLivedService.isTokenValid(token)).isFalse();
    }

    // Protects against malformed input: a string that isn't a JWT at all must fail
    // validation cleanly (no exception escaping) rather than being treated as valid.
    @Test
    void isTokenValid_falseForGarbageToken() {
        assertThat(jwtService.isTokenValid("not-a-real-jwt")).isFalse();
    }

    // Protects the signature check: a token signed with a different secret must be
    // rejected even though its structure/claims look fine. If this failed, anyone with
    // their own secret could forge tokens the app would accept.
    @Test
    void isTokenValid_falseWhenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-key-32bytes!", 3600_000L);
        String token = otherService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
