package com.memorylink.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("please-change-me-in-production-0123456789abcdef", 7200);

    @Test
    void tokenRoundTrip() {
        String token = jwtService.generateToken(42L, "13800138000", "USER");
        var claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("13800138000");
        assertThat(claims.get("uid", Number.class).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }
}
