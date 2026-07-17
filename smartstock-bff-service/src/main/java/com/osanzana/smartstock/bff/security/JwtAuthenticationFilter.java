package com.osanzana.smartstock.bff.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * Login/logout son públicos (permitAll en SecurityConfig), pero logout normalmente SÍ trae
     * la cookie de sesión — sin este bypass, la validación multi-tenant de abajo la rechazaría
     * (403 por falta de X-Comercio-ID) antes de que el controller pudiera invalidarla.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/v1/bff/auth/login") || path.equals("/api/v1/bff/auth/logout");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String jwt = resolveToken(request);
        final String username;

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            username = jwtUtils.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (jwtUtils.isTokenValid(jwt)) {
                    List<String> roles = jwtUtils.extractRoles(jwt);
                    Long tokenComercioId = jwtUtils.extractIdComercio(jwt);
                    String headerComercioId = request.getHeader("X-Comercio-ID");

                    logger.debug("JWT válido para usuario: " + username + ". Roles: " + roles + ". idComercio: " + tokenComercioId);

                    // Validación Multi-tenant
                    boolean isAdmin = roles.contains("ADMIN_SISTEMA");
                    if (!isAdmin) {
                        if (headerComercioId == null || headerComercioId.isEmpty()) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Header X-Comercio-ID es requerido para este rol\"}");
                            return;
                        }
                        try {
                            Long hId = Long.parseLong(headerComercioId);
                            if (tokenComercioId == null || !tokenComercioId.equals(hId)) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\": \"Acceso denegado: idComercio no coincide con el comercio solicitado\"}");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Header X-Comercio-ID invalido\"}");
                            return;
                        }
                    }

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            logger.error("Error al procesar el token JWT en BFF: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token inválido o expirado\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * El JWT llega principalmente por la cookie httpOnly "ss_token" (ver BffController.login),
     * con fallback al header Authorization: Bearer para clientes que no son el navegador
     * (Postman, tests, futura integración server-to-server).
     */
    private String resolveToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtUtils.AUTH_COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
