package com.osanzana.smartstock.alert.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "smartstock_super_secret_key_2026_jwt_token_must_be_long_enough";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", secret);
        ReflectionTestUtils.setField(jwtUtils, "expiration", 3600000L);
    }

    @Test
    void validateToken_Success() {
        String token = Jwts.builder()
                .claims(Map.of("rol", List.of("ADMIN")))
                .subject("test@test.cl")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertTrue(jwtUtils.validateToken(token, null));
        assertEquals("test@test.cl", jwtUtils.extractUsername(token));
        assertEquals(List.of("ADMIN"), jwtUtils.extractRoles(token));
    }

    @Test
    void extractRoles_SingleString() {
        String token = Jwts.builder()
                .claims(Map.of("rol", "USER"))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(List.of("USER"), jwtUtils.extractRoles(token));
    }
}
