package com.osanzana.smartstock.commerce.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String secret = "smartstock_super_secret_key_2026_jwt_token_must_be_long_enough";
    private final Long expiration = 3600000L;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", secret);
        ReflectionTestUtils.setField(jwtUtils, "expiration", expiration);
    }

    @Test
    void validateToken_Success() {
        String token = Jwts.builder()
                .subject("test@test.cl")
                .claim("rol", List.of("ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .authorities("ROLE_ADMIN")
                .build();

        assertTrue(jwtUtils.validateToken(token, userDetails));
        assertEquals("test@test.cl", jwtUtils.extractUsername(token));
        assertEquals(List.of("ADMIN"), jwtUtils.extractRoles(token));
    }

    @Test
    void validateToken_Expired() {
        String token = Jwts.builder()
                .subject("test@test.cl")
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtUtils.validateToken(token, null));
    }

    @Test
    void validateToken_InvalidSecret() {
        String token = Jwts.builder()
                .subject("test@test.cl")
                .signWith(Keys.hmacShaKeyFor("wrong_secret_key_must_be_long_enough_123".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtUtils.validateToken(token, null));
    }

    @Test
    void extractRoles_Null() {
        String token = Jwts.builder()
                .subject("test")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(Collections.emptyList(), jwtUtils.extractRoles(token));
    }

    @Test
    void extractRoles_SingleString() {
        String token = Jwts.builder()
                .claim("rol", "USER")
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(List.of("USER"), jwtUtils.extractRoles(token));
    }
}
