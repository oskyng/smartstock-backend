package com.osanzana.smartstock.bff.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${smartstock.jwt.secret:smartstock_super_secret_key_2026_jwt_token_must_be_long_enough}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> {
            Object roles = claims.get("rol");
            if (roles == null) {
                return java.util.Collections.emptyList();
            }
            if (roles instanceof String) {
                return List.of((String) roles);
            }
            return (List<String>) roles;
        });
    }

    public Long extractIdComercio(String token) {
        return extractClaim(token, claims -> {
            Object idComercio = claims.get("idComercio");
            if (idComercio == null) {
                return null;
            }
            if (idComercio instanceof Integer) {
                return ((Integer) idComercio).longValue();
            }
            if (idComercio instanceof Long) {
                return (Long) idComercio;
            }
            String value = idComercio.toString().trim();
            if (value.isEmpty()) {
                return null;
            }
            return Long.parseLong(value);
        });
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenValid(String token) {
        return validateToken(token);
    }

    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
