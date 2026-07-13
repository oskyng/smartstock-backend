package com.osanzana.smartstock.bff.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("BFF WireMock Integration Tests - End-to-End Communication")
public class BffWireMockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${smartstock.jwt.secret}")
    private String secret;

    private static final WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("smartstock.inventory-service.url", wireMockServer::baseUrl);
        registry.add("smartstock.alert-service.url", wireMockServer::baseUrl);
        registry.add("smartstock.finance-service.url", wireMockServer::baseUrl);
        registry.add("smartstock.commerce-service.url", wireMockServer::baseUrl);
        registry.add("smartstock.auth-service.url", wireMockServer::baseUrl);
        registry.add("smartstock.jwt.secret", () -> "smartstock_super_secret_key_2026_jwt_token_must_be_long_enough");
        registry.add("logging.level.com.osanzana.smartstock.bff.security", () -> "DEBUG");
    }

    private String generateToken(String username, String role, Long idComercio) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", List.of(role));
        claims.put("idComercio", idComercio != null ? idComercio : "");

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("A. Inventario: POST /lotes - 201 Created")
    void testInventory_CrearLote_Success() throws Exception {
        String token = generateToken("operador@test.com", "OPERADOR_INVENTARIO", 1L);
        String requestBody = "{\"idProducto\": 10, \"idProveedor\": 3, \"cantidadInicial\": 100, "
                + "\"costoUnitario\": 500, \"precioDinamico\": 900, \"fechaVencimiento\": \"2027-01-01\"}";
        String responseBody = "{\"id\": 1, \"nombreProducto\": \"Producto Test\", \"cantidadActual\": 100, "
                + "\"precioDinamico\": 900, \"fechaVencimiento\": \"2027-01-01\", \"estadoLote\": \"DISPONIBLE\"}";

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(
                urlEqualTo("/api/v1/inventario/lotes"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        MvcResult result = mockMvc.perform(post("/api/v1/bff/inventario/lotes")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Comercio-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();

        System.out.println("[DEBUG_LOG] Result Status: " + result.getResponse().getStatus());
        System.out.println("[DEBUG_LOG] Result Body: " + result.getResponse().getContentAsString());

        if (result.getRequest().isAsyncStarted()) {
            mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(content().json(responseBody));
        } else {
            assertEquals(201, result.getResponse().getStatus(), 
                "Respuesta fallida: " + result.getResponse().getContentAsString());
        }
    }

    @Test
    @DisplayName("A. Inventario: POST /lotes - 400 Bad Request (Error Propagado)")
    void testInventory_CrearLote_BadRequest() throws Exception {
        String token = generateToken("operador@test.com", "OPERADOR_INVENTARIO", 1L);
        String requestBody = "{\"idProducto\": 10, \"idProveedor\": 3, \"cantidadInicial\": 100, "
                + "\"costoUnitario\": 500, \"precioDinamico\": 900, \"fechaVencimiento\": \"2020-01-01\"}";
        String errorMsg = "La fecha no puede ser pasada";

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/inventario/lotes"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                        .withBody(errorMsg)));

        MvcResult result = mockMvc.perform(post("/api/v1/bff/inventario/lotes")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Comercio-ID", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMsg));
    }

    @Test
    @DisplayName("B. Alertas: GET /alertas - 200 OK")
    void testAlerts_ListarAlertas_Success() throws Exception {
        String token = generateToken("reponedor@test.com", "REPONEDOR_SALA", 1L);
        String responseBody = "[{\"id\": 1, \"mensaje\": \"Alerta 1\"}]";

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/v1/alertas/pendientes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        MvcResult result = mockMvc.perform(get("/api/v1/bff/alertas")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Comercio-ID", "1"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @Test
    @DisplayName("C. Finanzas: POST /reglas-depreciacion - 200 OK")
    void createDepreciationRule_asStoreManager_returnsOk() throws Exception {
        final String managerEmail = "gerente@test.com";
        final String managerRole = "GERENTE_TIENDA";
        final Long commerceId = 1L;
        final String commerceIdHeaderValue = commerceId.toString();
        final String financeServicePath = "/api/v1/reglas-depreciacion";
        final String bffPath = "/api/v1/bff/reglas-depreciacion";
        final String requestBody = "{\"idCategoria\": 1, \"diasCriticosMin\": 3, \"porcentajeDescuento\": 50.00}";
        final String responseBody = "{\"id\": 1, \"nombreCategoria\": \"Lácteos\", \"diasCriticosMin\": 3, \"porcentajeDescuento\": 50.00, \"nombreGerente\": \"Gerente Test\", \"activa\": 1}";
        final String authorizationHeader = "Bearer " + generateToken(managerEmail, managerRole, commerceId);

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo(financeServicePath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        MvcResult result = mockMvc.perform(post(bffPath)
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .header("X-Comercio-ID", commerceIdHeaderValue)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @Test
    @DisplayName("D. Comercio: POST /comercios - 201 Created")
    void testCommerce_CrearComercio_Success() throws Exception {
        String token = generateToken("admin@smartstock.cl", "ADMIN_SISTEMA", null);
        String requestBody = "{\"rutEmpresa\": \"76.123.456-7\", \"razonSocial\": \"Sucursal 5\", \"rubro\": \"Retail\"}";
        String responseBody = "{\"id\": 5, \"rutEmpresa\": \"76.123.456-7\", \"razonSocial\": \"Sucursal 5\", \"rubro\": \"Retail\", \"estado\": \"ACTIVO\"}";

        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/api/v1/comercios"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(responseBody)));

        MvcResult result = mockMvc.perform(post("/api/v1/bff/comercios")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn();

        if (result.getRequest().isAsyncStarted()) {
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isCreated())
                    .andExpect(content().json(responseBody));
        } else {
            // Si no es asíncrono, debería haber respondido ya
            int status = result.getResponse().getStatus();
            if (status == 201) {
                // Ya pasó
            } else {
                throw new AssertionError("Status expected:<201> but was:<" + status + ">");
            }
        }
    }
}
