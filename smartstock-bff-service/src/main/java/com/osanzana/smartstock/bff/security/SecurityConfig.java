package com.osanzana.smartstock.bff.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.bff.dto.ErrorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/bff/auth/login",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // B. OPERACIONES EXCLUSIVAS DEL ROL [ADMIN_SISTEMA]
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bff/comercios").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bff/comercios").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/bff/comercios/**").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bff/usuarios").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")
                        
                        // C. OPERACIONES DEL ROL [GERENTE_TIENDA]
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bff/dashboard").hasRole("GERENTE_TIENDA")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bff/reglas-depreciacion").hasRole("GERENTE_TIENDA")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bff/reglas-depreciacion").hasRole("GERENTE_TIENDA")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/v1/bff/reglas-depreciacion/**").hasRole("GERENTE_TIENDA")
                        
                        // D. OPERACIONES DEL ROL [OPERADOR_INVENTARIO]
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bff/productos").hasRole("OPERADOR_INVENTARIO")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bff/productos").hasRole("OPERADOR_INVENTARIO")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bff/inventario/lotes").hasRole("OPERADOR_INVENTARIO")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/bff/inventario/lotes").hasRole("OPERADOR_INVENTARIO")
                        
                        // E. OPERACIONES DEL ROL [REPONEDOR_SALA]
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/bff/alertas").hasRole("REPONEDOR_SALA")
                        
                        // F. OPERACIÓN COMPARTIDA
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/bff/alertas/*/atender").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA", "REPONEDOR_SALA")
                        
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponseDTO error = ErrorResponseDTO.builder()
                    .status(HttpStatus.UNAUTHORIZED.value())
                    .message("BFF EntryPoint: No autorizado - " + authException.getMessage())
                    .timestamp(LocalDateTime.now())
                    .path(request.getRequestURI())
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponseDTO error = ErrorResponseDTO.builder()
                    .status(HttpStatus.FORBIDDEN.value())
                    .message("Acceso denegado: No tiene permisos suficientes")
                    .timestamp(LocalDateTime.now())
                    .path(request.getRequestURI())
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(error));
        };
    }

    private static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
                throws ServletException, IOException {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
            filterChain.doFilter(request, response);
        }
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*")); // En producción, especificar el dominio de Angular
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Comercio-ID"));
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
