package com.osanzana.smartstock.inventory.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            userEmail = jwtUtils.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtils.validateToken(jwt, null)) {
                    List<String> roles = jwtUtils.extractRoles(jwt);
                    Long tokenComercioId = jwtUtils.extractIdComercio(jwt);
                    String headerComercioId = request.getHeader("X-Comercio-ID");

                    log.debug("JWT válido para usuario: {}. Roles: {}. Token ComercioId: {}", userEmail, roles, tokenComercioId);

                    // Validación Multi-tenant
                    if (!roles.contains("ADMIN_SISTEMA")) {
                        if (headerComercioId == null || headerComercioId.isEmpty()) {
                            log.warn("Acceso denegado: Header X-Comercio-ID ausente para rol {}", roles);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("Acceso denegado: Header X-Comercio-ID requerido");
                            return;
                        }
                        if (tokenComercioId == null || !tokenComercioId.equals(Long.parseLong(headerComercioId))) {
                            log.warn("Acceso denegado: Comercio ID no coincide. Token: {}, Header: {}", tokenComercioId, headerComercioId);
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write("Acceso denegado: Comercio ID no coincide");
                            return;
                        }
                    }

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userEmail,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Error al procesar el token JWT: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Token invalido o expirado");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
