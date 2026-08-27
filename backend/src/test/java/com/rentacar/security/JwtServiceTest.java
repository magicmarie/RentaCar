package com.rentacar.security;

import com.rentacar.entity.Role;
import com.rentacar.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-only-secret-key-at-least-32-bytes-long!!";

    private JwtService jwtService;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 3600_000L);
        User user = User.builder().id(1L).email("jane@example.com").role(Role.CUSTOMER).active(true).build();
        principal = new UserPrincipal(user);
    }

    @Test
    void generateToken_producesTokenWithCorrectSubject() {
        String token = jwtService.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo("jane@example.com");
    }

    @Test
    void isTokenValid_trueForFreshlyIssuedToken() {
        String token = jwtService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_falseForExpiredToken() {
        JwtService shortLivedService = new JwtService(SECRET, -1000L);
        String token = shortLivedService.generateToken(principal);

        assertThat(shortLivedService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_falseForGarbageToken() {
        assertThat(jwtService.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isTokenValid_falseWhenSignedWithDifferentSecret() {
        JwtService otherService = new JwtService("a-completely-different-secret-key-32bytes!", 3600_000L);
        String token = otherService.generateToken(principal);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
