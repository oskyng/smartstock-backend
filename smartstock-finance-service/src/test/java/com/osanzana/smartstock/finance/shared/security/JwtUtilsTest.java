package com.osanzana.smartstock.finance.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
    void generateToken_Success() {
        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        String token = jwtUtils.generateToken(userDetails, Map.of("rol", "ROLE_USER"));
        assertNotNull(token);
        assertEquals("test@test.cl", jwtUtils.extractUsername(token));
        assertEquals(List.of("ROLE_USER"), jwtUtils.extractRoles(token));
    }

    @Test
    void validateToken_Success() {
        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        String token = jwtUtils.generateToken(userDetails, Collections.emptyMap());
        assertTrue(jwtUtils.validateToken(token, userDetails));
        assertTrue(jwtUtils.validateToken(token, null));
    }

    @Test
    void validateToken_WrongUser() {
        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .authorities("ROLE_USER")
                .build();
        
        UserDetails otherUser = User.builder()
                .username("other@test.cl")
                .password("password")
                .build();

        String token = jwtUtils.generateToken(userDetails, Collections.emptyMap());
        assertFalse(jwtUtils.validateToken(token, otherUser));
    }

    @Test
    void extractRoles_AsString() {
        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .build();

        String token = jwtUtils.generateToken(userDetails, Map.of("rol", "ROLE_ADMIN"));
        List<String> roles = jwtUtils.extractRoles(token);
        assertEquals(List.of("ROLE_ADMIN"), roles);
    }

    @Test
    void extractRoles_AsList() {
        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .build();

        String token = jwtUtils.generateToken(userDetails, Map.of("rol", List.of("ROLE_USER", "ROLE_ADMIN")));
        List<String> roles = jwtUtils.extractRoles(token);
        assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), roles);
    }

    @Test
    void isTokenExpired_False() {
        UserDetails userDetails = User.builder()
                .username("test@test.cl")
                .password("password")
                .build();

        String token = jwtUtils.generateToken(userDetails, Collections.emptyMap());
        // Since expiration is 1h, it shouldn't be expired
        Boolean expired = ReflectionTestUtils.invokeMethod(jwtUtils, "isTokenExpired", token);
        assertFalse(expired);
    }
}
