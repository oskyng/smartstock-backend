package com.osanzana.smartstock.bff.security;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class BffSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SmartStockClient smartStockClient;

    @MockBean
    private AuditStreamService auditStreamService;

    @Value("${smartstock.jwt.secret:smartstock_super_secret_key_2026_jwt_token_must_be_long_enough}")
    private String secret;

    private String generateToken(String username, String role) {
        return Jwts.builder()
                .claims(Map.of("rol", List.of(role)))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String generateToken(String username, String role, Long idComercio) {
        return Jwts.builder()
                .claims(Map.of("rol", List.of(role), "idComercio", idComercio))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void testCrearUsuario_AdminSistema_Success() throws Exception {
        String token = generateToken("admin_sys@test.com", "ADMIN_SISTEMA");
        when(smartStockClient.crearUsuario(any(), any())).thenReturn(Mono.just("Created"));

        mockMvc.perform(post("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testCrearUsuario_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@test.com", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.crearUsuario(any(), any())).thenReturn(Mono.just("Created"));

        mockMvc.perform(post("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testCrearUsuario_Operador_Forbidden() throws Exception {
        String token = generateToken("operador@test.com", "OPERADOR_INVENTARIO", 1L);

        mockMvc.perform(post("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", "1")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testListarComercios_Admin_Forbidden() throws Exception {
        String token = generateToken("admin@test.com", "ADMIN");
        when(smartStockClient.listarComercios()).thenReturn(Mono.just("[]"));

        mockMvc.perform(get("/api/v1/bff/comercios")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testListarComercios_AdminSistema_Success() throws Exception {
        String token = generateToken("admin_sys@test.com", "ADMIN_SISTEMA");
        when(smartStockClient.listarComercios()).thenReturn(Mono.just("[]"));

        mockMvc.perform(get("/api/v1/bff/comercios")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testListarComercios_NoToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/bff/comercios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testListarComercios_Gerente_Forbidden() throws Exception {
        String token = generateToken("gerente@test.com", "GERENTE_TIENDA");

        mockMvc.perform(get("/api/v1/bff/comercios")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
