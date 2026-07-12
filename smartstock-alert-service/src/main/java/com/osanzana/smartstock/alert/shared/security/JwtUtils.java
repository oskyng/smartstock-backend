package com.osanzana.smartstock.alert.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${smartstock.jwt.secret:smartstock_super_secret_key_2026_jwt_token_must_be_long_enough}")
    private String secret;

    @Value("${smartstock.jwt.expiration:86400000}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
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
            if (idComercio instanceof String) {
                return Long.parseLong((String) idComercio);
            }
            return (Long) idComercio;
        });
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (userDetails != null) {
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        }
        return !isTokenExpired(token);
    }

    private Boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
