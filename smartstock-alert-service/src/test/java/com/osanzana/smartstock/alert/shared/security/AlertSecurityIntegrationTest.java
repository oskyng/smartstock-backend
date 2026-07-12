package com.osanzana.smartstock.alert.shared.security;

import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import com.osanzana.smartstock.alert.alertas.web.AlertaController;
import com.osanzana.smartstock.alert.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el filtro JWT del alert-service quede correctamente habilitado
 * (no en modo permitAll) y que la regla de aislamiento multi-tenant
 * (X-Comercio-ID vs claim idComercio del token) se aplique sobre sus propios
 * endpoints, de forma independiente al BFF.
 */
@WebMvcTest(controllers = AlertaController.class)
@Import({
        HttpSecurityConfig.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtUtils.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
@DisplayName("Alert Service - Security Integration Tests")
class AlertSecurityIntegrationTest {

    private static final String SECRET = "smartstock_super_secret_key_2026_jwt_token_must_be_long_enough";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertaService alertaService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    private String generateToken(String role, Long idComercio) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", List.of(role));
        if (idComercio != null) {
            claims.put("idComercio", idComercio);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(role.toLowerCase() + "@test.cl")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("GET /pendientes sin token -> 401 Unauthorized")
    void listarPendientes_SinToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/alertas/pendientes").header("X-Comercio-ID", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /pendientes con REPONEDOR_SALA y comercio correcto -> 200 OK")
    void listarPendientes_ReponedorSala_ComercioCorrecto_Success() throws Exception {
        when(alertaService.listarPendientesPorComercio(1L)).thenReturn(List.of(
                AlertaResponseDTO.builder()
                        .id(1L)
                        .mensaje("Alerta de prueba")
                        .estado("PENDIENTE")
                        .fechaLimite(LocalDateTime.now().plusHours(24))
                        .build()
        ));

        mockMvc.perform(get("/api/v1/alertas/pendientes")
                        .header("Authorization", "Bearer " + generateToken("REPONEDOR_SALA", 1L))
                        .header("X-Comercio-ID", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /pendientes con idComercio del token distinto al header -> 403 Forbidden (aislamiento multi-tenant)")
    void listarPendientes_TenantMismatch_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/alertas/pendientes")
                        .header("Authorization", "Bearer " + generateToken("REPONEDOR_SALA", 1L))
                        .header("X-Comercio-ID", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /pendientes sin header X-Comercio-ID para rol no-admin -> 403 Forbidden")
    void listarPendientes_SinHeaderComercio_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/alertas/pendientes")
                        .header("Authorization", "Bearer " + generateToken("REPONEDOR_SALA", 1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /{id}/atender con ADMIN_SISTEMA (sin idComercio) -> 204 No Content")
    void atenderAlerta_AdminSistema_Success() throws Exception {
        mockMvc.perform(put("/api/v1/alertas/1/atender")
                        .header("Authorization", "Bearer " + generateToken("ADMIN_SISTEMA", null)))
                .andExpect(status().isNoContent());
    }
}
