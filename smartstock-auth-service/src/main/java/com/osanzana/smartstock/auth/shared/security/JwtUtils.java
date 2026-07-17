package com.osanzana.smartstock.auth.shared.security;

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

    @Value("${smartstock.jwt.secret:dev_only_placeholder_secret_never_used_in_production_set_env_var}")
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

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Token de corta duración exclusivo para el paso "restablecer contraseña" del flujo de
     * recuperación. Lleva el claim "purpose" para que reset-password pueda rechazar un token de
     * login normal (que también pasaría la autenticación genérica del filtro JWT al no llevar
     * roles) y solo aceptar uno emitido específicamente tras verificar el código de 6 dígitos.
     */
    public String generarTokenRestablecimiento(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("purpose", "PASSWORD_RESET")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 600_000)) // 10 minutos
                .signWith(getSigningKey())
                .compact();
    }

    public String extractPurpose(String token) {
        return extractClaim(token, claims -> (String) claims.get("purpose"));
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
