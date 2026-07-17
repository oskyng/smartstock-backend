package com.osanzana.smartstock.bff.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.bff.dto.ErrorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @org.springframework.beans.factory.annotation.Value("${smartstock.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // El JWT viaja en una cookie httpOnly con SameSite=Strict (ver BffController.login):
                // el navegador nunca la adjunta en peticiones cross-site, así que no hace falta el
                // mecanismo de token CSRF de Spring Security además de eso.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // A. Acceso público
                        .requestMatchers(
                                "/api/v1/bff/auth/login",
                                "/api/v1/bff/auth/logout",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // B. [ADMIN_SISTEMA] - Gestión global de comercios (Commerce Service)
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/comercios/**").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/comercios/**").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bff/comercios/**").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bff/comercios/**").hasRole("ADMIN_SISTEMA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/usuarios").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/usuarios").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/bff/usuarios/**").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bff/usuarios/**").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bff/usuarios/**").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/roles").hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA")

                        // C. [GERENTE_TIENDA] - Dashboard y reglas de depreciación
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/dashboard").hasRole("GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/audit/stream").hasRole("GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/reglas-depreciacion/**").hasRole("GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/reglas-depreciacion/**").hasRole("GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/bff/reglas-depreciacion/**").hasRole("GERENTE_TIENDA")

                        // D. [OPERADOR_INVENTARIO] - Productos e inventario de lotes
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/productos/**").hasRole("OPERADOR_INVENTARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/productos/**").hasRole("OPERADOR_INVENTARIO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/categorias/**").hasAnyRole("OPERADOR_INVENTARIO", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/categorias/**").hasAnyRole("OPERADOR_INVENTARIO", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/proveedores/**").hasAnyRole("OPERADOR_INVENTARIO", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/proveedores/**").hasAnyRole("OPERADOR_INVENTARIO", "GERENTE_TIENDA")
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/inventario/lotes/**").hasRole("OPERADOR_INVENTARIO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/bff/inventario/lotes/**").hasRole("OPERADOR_INVENTARIO")

                        // E. [REPONEDOR_SALA] - Alertas de sala
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/alertas").hasRole("REPONEDOR_SALA")

                        // E2. [GERENTE_TIENDA] - Panel de control y auditoría de alertas
                        .requestMatchers(HttpMethod.GET, "/api/v1/bff/alertas/auditoria").hasRole("GERENTE_TIENDA")

                        // F. Resolución de alarmas (CA-07) - Operación compartida
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/bff/alertas/*/atender")
                        .hasAnyRole("ADMIN_SISTEMA", "GERENTE_TIENDA", "REPONEDOR_SALA")

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

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Comercio-ID"));
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));
        // Necesario para que el navegador adjunte/reciba la cookie httpOnly de sesión.
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
