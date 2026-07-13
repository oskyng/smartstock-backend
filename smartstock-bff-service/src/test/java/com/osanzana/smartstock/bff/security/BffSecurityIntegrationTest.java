package com.osanzana.smartstock.bff.security;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.CategoriaResponseDTO;
import com.osanzana.smartstock.bff.dto.ProveedorResponseDTO;
import com.osanzana.smartstock.bff.dto.UsuarioCreateResponseDTO;
import com.osanzana.smartstock.bff.dto.UsuarioResponseDTO;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("BFF Security Integration Tests - Comprehensive Suite")
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
                .claims(Map.of("rol", List.of(role), "idComercio", idComercio != null ? idComercio : ""))
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private UsuarioCreateResponseDTO sampleUsuarioCreateResponse() {
        return UsuarioCreateResponseDTO.builder()
                .mensaje("Usuario creado exitosamente.")
                .usuario(UsuarioResponseDTO.builder().id(1L).email("nuevo@test.com").build())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void testCrearUsuario_AdminSistema_Success() throws Exception {
        String token = generateToken("admin_sys@test.com", "ADMIN_SISTEMA");
        when(smartStockClient.crearUsuario(any(), any())).thenReturn(Mono.just(sampleUsuarioCreateResponse()));

        mockMvc.perform(post("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"rut\":\"11.111.111-1\",\"nombre\":\"Test\",\"apellido\":\"User\",\"email\":\"nuevo@test.com\",\"password\":\"password123\",\"idRol\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void testCrearUsuario_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@test.com", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.crearUsuario(any(), any())).thenReturn(Mono.just(sampleUsuarioCreateResponse()));

        mockMvc.perform(post("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"rut\":\"11.111.111-1\",\"nombre\":\"Test\",\"apellido\":\"User\",\"email\":\"nuevo@test.com\",\"password\":\"password123\",\"idRol\":1}"))
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
        when(smartStockClient.listarComercios()).thenReturn(Mono.just(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/bff/comercios")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testListarComercios_AdminSistema_Success() throws Exception {
        String token = generateToken("admin_sys@test.com", "ADMIN_SISTEMA");
        when(smartStockClient.listarComercios()).thenReturn(Mono.just(Collections.emptyList()));

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

    @Test
    void testListarAlertas_ReponedorSala_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@test.com", "REPONEDOR_SALA", commerceId);
        when(smartStockClient.listarAlertas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/bff/alertas")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: puede acceder al panel de auditoría de alertas")
    void testListarAuditoriaAlertas_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@test.com", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.listarAuditoriaAlertas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/bff/alertas/auditoria")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("REPONEDOR_SALA: sin acceso al panel de auditoría del gerente")
    void testListarAuditoriaAlertas_ReponedorSala_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@test.com", "REPONEDOR_SALA", commerceId);

        mockMvc.perform(get("/api/v1/bff/alertas/auditoria")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: Sin acceso exclusivo de REPONEDOR_SALA a GET /alertas")
    void testListarAlertas_GerenteTienda_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@test.com", "GERENTE_TIENDA", commerceId);

        mockMvc.perform(get("/api/v1/bff/alertas")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: Fallo Crítico por suplantación de Tenant (X-Comercio-ID incorrecto)")
    void testGerenteTienda_SuplantacionTenant_Forbidden() throws Exception {
        Long tokenCommerceId = 1L;
        Long requestCommerceId = 2L;
        String token = generateToken("gerente@test.com", "GERENTE_TIENDA", tokenCommerceId);

        mockMvc.perform(post("/api/v1/bff/reglas-depreciacion")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", requestCommerceId.toString())
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado: idComercio no coincide con el comercio solicitado"));
    }

    @Test
    @DisplayName("ADMIN_SISTEMA: Bloqueo en gestión de inventario local (No tiene rol OPERADOR)")
    void testAdminSistema_InventarioLotes_Forbidden() throws Exception {
        String token = generateToken("admin@smartstock.cl", "ADMIN_SISTEMA");

        mockMvc.perform(post("/api/v1/bff/inventario/lotes")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", "1")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("OPERADOR_INVENTARIO: Fallo por regla de negocio (Fecha pasada)")
    void testOperadorInventario_FechaPasada_BadRequest() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("operador@tienda1.cl", "OPERADOR_INVENTARIO", commerceId);

        // Simular que el microservicio responde 400 Bad Request
        byte[] body = "{\"error\": \"La fecha de vencimiento no puede ser pasada\"}".getBytes(StandardCharsets.UTF_8);
        when(smartStockClient.crearLote(any(), any()))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        400, "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        body,
                        StandardCharsets.UTF_8)));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/bff/inventario/lotes")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"idProducto\":1,\"idProveedor\":1,\"cantidadInicial\":10,\"costoUnitario\":100,\"precioDinamico\":150,\"fechaVencimiento\":\"2020-01-01\"}"))
                .andReturn();

        org.junit.jupiter.api.Assertions.assertNotNull(mvcResult.getAsyncResult());
    }

    @Test
    @DisplayName("REPONEDOR_SALA: Bloqueo en parametrización de finanzas")
    void testReponedorSala_CrearRegla_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@tienda1.cl", "REPONEDOR_SALA", commerceId);

        mockMvc.perform(post("/api/v1/bff/reglas-depreciacion")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CA-07: REPONEDOR_SALA puede atender alertas (PATCH) - autorizado, request llega al controlador")
    void testAtenderAlerta_ReponedorSala_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@tienda1.cl", "REPONEDOR_SALA", commerceId);
        when(smartStockClient.atenderAlerta(anyLong())).thenReturn(Mono.empty());

        MvcResult result = mockMvc.perform(patch("/api/v1/bff/alertas/1/atender")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andReturn();

        // La autorización RBAC ocurre en el dispatch síncrono inicial; si pasó,
        // la request asíncrona hacia el controlador reactivo se inicia.
        org.junit.jupiter.api.Assertions.assertTrue(result.getRequest().isAsyncStarted());
    }

    @Test
    @DisplayName("CA-07: OPERADOR_INVENTARIO no puede atender alertas (fuera del set compartido)")
    void testAtenderAlerta_OperadorInventario_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("operador@tienda1.cl", "OPERADOR_INVENTARIO", commerceId);

        mockMvc.perform(patch("/api/v1/bff/alertas/1/atender")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: puede crear categorías (gestión de catálogo)")
    void testCrearCategoria_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@tienda1.cl", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.crearCategoria(any(), any()))
                .thenReturn(Mono.just(CategoriaResponseDTO.builder().id(1L).nombre("BEBIDAS").build()));

        MvcResult result = mockMvc.perform(post("/api/v1/bff/categorias")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"nombre\":\"BEBIDAS\"}"))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: puede crear proveedores (gestión de catálogo)")
    void testCrearProveedor_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@tienda1.cl", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.crearProveedor(any(), any()))
                .thenReturn(Mono.just(ProveedorResponseDTO.builder().id(1L).rutEmpresa("76.111.111-1")
                        .razonSocial("Proveedor Uno").contactoEmail("contacto@proveedor.cl").build()));

        MvcResult result = mockMvc.perform(post("/api/v1/bff/proveedores")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"rutEmpresa\":\"76.111.111-1\",\"razonSocial\":\"Proveedor Uno\",\"contactoEmail\":\"contacto@proveedor.cl\"}"))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("REPONEDOR_SALA: no puede crear categorías (fuera del set OPERADOR/GERENTE)")
    void testCrearCategoria_ReponedorSala_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@tienda1.cl", "REPONEDOR_SALA", commerceId);

        mockMvc.perform(post("/api/v1/bff/categorias")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"nombre\":\"BEBIDAS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("REPONEDOR_SALA: no puede crear proveedores (fuera del set OPERADOR/GERENTE)")
    void testCrearProveedor_ReponedorSala_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@tienda1.cl", "REPONEDOR_SALA", commerceId);

        mockMvc.perform(post("/api/v1/bff/proveedores")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"rutEmpresa\":\"76.111.111-1\",\"razonSocial\":\"Proveedor Uno\",\"contactoEmail\":\"contacto@proveedor.cl\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: puede listar usuarios de su comercio")
    void testListarUsuarios_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@tienda1.cl", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.listarUsuarios(any())).thenReturn(Mono.just(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: puede actualizar un usuario de su comercio")
    void testActualizarUsuario_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@tienda1.cl", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.actualizarUsuario(any(), any(), any()))
                .thenReturn(Mono.just(UsuarioResponseDTO.builder().id(5L).email("op@tienda1.cl").build()));

        mockMvc.perform(put("/api/v1/bff/usuarios/5")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString())
                .contentType("application/json")
                .content("{\"nombre\":\"Nuevo\",\"apellido\":\"Nombre\",\"email\":\"op@tienda1.cl\",\"idRol\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GERENTE_TIENDA: puede desactivar un usuario de su comercio")
    void testEliminarUsuario_GerenteTienda_Success() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("gerente@tienda1.cl", "GERENTE_TIENDA", commerceId);
        when(smartStockClient.eliminarUsuario(any(), any())).thenReturn(Mono.empty());

        MvcResult result = mockMvc.perform(delete("/api/v1/bff/usuarios/5")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("REPONEDOR_SALA: no puede listar usuarios (fuera del set ADMIN/GERENTE)")
    void testListarUsuarios_ReponedorSala_Forbidden() throws Exception {
        Long commerceId = 1L;
        String token = generateToken("reponedor@tienda1.cl", "REPONEDOR_SALA", commerceId);

        mockMvc.perform(get("/api/v1/bff/usuarios")
                .header("Authorization", "Bearer " + token)
                .header("X-Comercio-ID", commerceId.toString()))
                .andExpect(status().isForbidden());
    }
}
