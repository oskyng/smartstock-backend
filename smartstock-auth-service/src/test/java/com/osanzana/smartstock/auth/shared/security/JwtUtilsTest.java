package com.osanzana.smartstock.auth.shared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "smartstock_super_secret_key_2026_jwt_token_must_be_long");
        ReflectionTestUtils.setField(jwtUtils, "expiration", 3600000L);
        
        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    @Test
    void generateAndValidateToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", "ADMIN");
        
        String token = jwtUtils.generateToken(userDetails, claims);
        assertNotNull(token);
        
        assertTrue(jwtUtils.validateToken(token, userDetails));
        assertEquals("test@example.com", jwtUtils.extractUsername(token));
        assertEquals("ADMIN", jwtUtils.extractClaim(token, c -> c.get("rol")));
    }

    @Test
    void isTokenExpired_False() {
        String token = jwtUtils.generateToken(userDetails, new HashMap<>());
        Boolean isExpired = ReflectionTestUtils.invokeMethod(jwtUtils, "isTokenExpired", token);
        assertFalse(isExpired);
    }
}
