package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.CambiarContrasenaRequestDTO;
import com.osanzana.smartstock.bff.dto.ComercioRequestDTO;
import com.osanzana.smartstock.bff.dto.ComercioResponseDTO;
import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
import com.osanzana.smartstock.bff.dto.LoteResponseDTO;
import com.osanzana.smartstock.bff.dto.ProductoResponseDTO;
import com.osanzana.smartstock.bff.dto.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.bff.dto.UsuarioCreateResponseDTO;
import com.osanzana.smartstock.bff.dto.UsuarioRequestDTO;
import com.osanzana.smartstock.bff.dto.UsuarioResponseDTO;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BffControllerTest {

    @Mock
    private SmartStockClient client;

    @Mock
    private AuditStreamService auditStreamService;

    @InjectMocks
    private BffController bffController;

    @Test
    void login_Success_SetsHttpOnlyCookieAndStripsTokenFromBody() {
        ReflectionTestUtils.setField(bffController, "jwtExpirationMs", 86_400_000L);
        ReflectionTestUtils.setField(bffController, "cookieSecure", false);

        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .email("test@test.com")
                .password("pass")
                .build();
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token("jwt")
                .email("test@test.com")
                .rol("ADMIN")
                .idComercio(1L)
                .build();
        when(client.login(any(LoginRequestDTO.class))).thenReturn(Mono.just(loginResponse));

        MockHttpServletResponse response = new MockHttpServletResponse();

        StepVerifier.create(bffController.login(loginRequest, response))
                .assertNext(body -> {
                    assertNull(body.getToken());
                    assertEquals("test@test.com", body.getEmail());
                    assertEquals("ADMIN", body.getRol());
                    assertEquals(1L, body.getIdComercio());
                })
                .verifyComplete();

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("ss_token=jwt"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
    }

    @Test
    void logout_Success_ClearsCookie() {
        ReflectionTestUtils.setField(bffController, "cookieSecure", false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = bffController.logout(response);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("ss_token="));
        assertTrue(setCookie.contains("Max-Age=0"));
    }

    @Test
    void crearUsuario_Success() {
        UsuarioRequestDTO usuarioRequest = UsuarioRequestDTO.builder()
                .rut("11.111.111-1")
                .nombre("Test")
                .apellido("User")
                .email("test@test.com")
                .password("password123")
                .idRol(1L)
                .build();
        UsuarioCreateResponseDTO expected = UsuarioCreateResponseDTO.builder()
                .mensaje("Usuario creado exitosamente.")
                .usuario(UsuarioResponseDTO.builder().id(1L).email("test@test.com").build())
                .timestamp(System.currentTimeMillis())
                .build();
        when(client.crearUsuario(any(), anyLong())).thenReturn(Mono.just(expected));

        StepVerifier.create(bffController.crearUsuario(1L, usuarioRequest))
                .expectNext(expected)
                .verifyComplete();
    }

    @Test
    void reactivarUsuario_Success() {
        UsuarioResponseDTO expected = UsuarioResponseDTO.builder().id(5L).activo(1).build();
        when(client.reactivarUsuario(5L, 1L)).thenReturn(Mono.just(expected));

        StepVerifier.create(bffController.reactivarUsuario(5L, 1L))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(expected, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    void cambiarContrasenaUsuario_Success() {
        CambiarContrasenaRequestDTO request = CambiarContrasenaRequestDTO.builder()
                .nuevaContrasena("claveNueva123")
                .build();
        when(client.cambiarContrasenaUsuario(5L, 1L, request)).thenReturn(Mono.empty());

        StepVerifier.create(bffController.cambiarContrasenaUsuario(5L, 1L, request))
                .assertNext(response -> assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode()))
                .verifyComplete();
    }

    @Test
    void listarComercios_Success() {
        when(client.listarComercios()).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(bffController.listarComercios())
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(Collections.emptyList(), response.getBody());
                })
                .verifyComplete();
    }

    @Test
    void crearComercio_Success() {
        ComercioRequestDTO request = ComercioRequestDTO.builder()
                .rutEmpresa("76.123.456-7")
                .razonSocial("Empresa Test")
                .build();
        ComercioResponseDTO expected = ComercioResponseDTO.builder().id(1L).razonSocial("Empresa Test").build();
        when(client.crearComercio(any())).thenReturn(Mono.just(expected));

        StepVerifier.create(bffController.crearComercio(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());
                    assertEquals(expected, response.getBody());
                })
                .verifyComplete();
    }

    @Test
    void getDashboard_Success() {
        when(client.listarProductos(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarLotesInterno(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarAlertas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarReglas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(bffController.getDashboard(1L))
                .assertNext(dashboard -> {
                    assertEquals(Collections.emptyList(), dashboard.getProductos());
                    assertEquals(Collections.emptyList(), dashboard.getLotesRecientes());
                    assertEquals(Collections.emptyList(), dashboard.getAlertasPendientes());
                    assertEquals(Collections.emptyList(), dashboard.getReglasActivas());
                    assertEquals(BigDecimal.ZERO, dashboard.getCapitalEnRiesgo());
                })
                .verifyComplete();
    }

    @Test
    void getDashboard_CalculaCapitalEnRiesgoSoloParaLotesEnRiesgo() {
        LoteResponseDTO loteEnRiesgo = LoteResponseDTO.builder()
                .id(1L)
                .nombreCategoria("Lácteos")
                .cantidadActual(10)
                .costoUnitario(new BigDecimal("500"))
                .fechaVencimiento(LocalDate.now().plusDays(2))
                .build();
        LoteResponseDTO loteFueraDeRiesgo = LoteResponseDTO.builder()
                .id(2L)
                .nombreCategoria("Lácteos")
                .cantidadActual(20)
                .costoUnitario(new BigDecimal("300"))
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build();
        ReglaDepreciacionResponseDTO regla = ReglaDepreciacionResponseDTO.builder()
                .nombreCategoria("Lácteos")
                .diasCriticosMin(5)
                .activa(1)
                .build();

        when(client.listarProductos(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarLotesInterno(anyLong())).thenReturn(Mono.just(List.of(loteEnRiesgo, loteFueraDeRiesgo)));
        when(client.listarAlertas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarReglas(anyLong())).thenReturn(Mono.just(List.of(regla)));

        StepVerifier.create(bffController.getDashboard(1L))
                .assertNext(dashboard -> assertEquals(new BigDecimal("5000"), dashboard.getCapitalEnRiesgo()))
                .verifyComplete();
    }

    @Test
    void listarAlertas_Success() {
        Long comercioId = 1L;
        when(client.listarAlertas(eq(comercioId))).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(bffController.listarAlertas(comercioId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(Collections.emptyList(), response.getBody());
                })
                .verifyComplete();
    }

    @Test
    void atenderAlerta_Success() {
        when(client.atenderAlerta(anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(bffController.atenderAlerta(1L))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                })
                .verifyComplete();
    }

    @Test
    void getAuditStream_Success() {
        com.osanzana.smartstock.bff.dto.AlertaEscaladaEvent event = com.osanzana.smartstock.bff.dto.AlertaEscaladaEvent.builder()
                .alertaId(1L)
                .productoNombre("Producto Test")
                .build();
        org.springframework.http.codec.ServerSentEvent<com.osanzana.smartstock.bff.dto.AlertaEscaladaEvent> sse =
                org.springframework.http.codec.ServerSentEvent.builder(event).build();

        when(auditStreamService.getAuditStream(1L)).thenReturn(Flux.just(sse));

        StepVerifier.create(bffController.getAuditStream(1L))
                .expectNext(sse)
                .verifyComplete();
    }

    @Test
    void listarProductos_Success() {
        List<ProductoResponseDTO> productos = Collections.emptyList();
        when(client.listarProductos(anyLong())).thenReturn(Mono.just(productos));

        StepVerifier.create(bffController.listarProductos(1L))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(productos, response.getBody());
                })
                .verifyComplete();
    }
}
