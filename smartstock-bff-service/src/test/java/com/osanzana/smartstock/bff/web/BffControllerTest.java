package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.ComercioRequestDTO;
import com.osanzana.smartstock.bff.dto.ComercioResponseDTO;
import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
import com.osanzana.smartstock.bff.dto.ProductoResponseDTO;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void login_Success() {
        LoginRequestDTO loginRequest = LoginRequestDTO.builder()
                .email("test@test.com")
                .password("pass")
                .build();
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token("jwt")
                .email("test@test.com")
                .rol("ADMIN")
                .build();
        when(client.login(any(LoginRequestDTO.class))).thenReturn(Mono.just(loginResponse));

        StepVerifier.create(bffController.login(loginRequest))
                .expectNext(loginResponse)
                .verifyComplete();
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
        when(client.listarLotes(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarAlertas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));
        when(client.listarReglas(anyLong())).thenReturn(Mono.just(Collections.emptyList()));

        StepVerifier.create(bffController.getDashboard(1L))
                .assertNext(dashboard -> {
                    assertEquals(Collections.emptyList(), dashboard.getProductos());
                    assertEquals(Collections.emptyList(), dashboard.getLotesRecientes());
                    assertEquals(Collections.emptyList(), dashboard.getAlertasPendientes());
                    assertEquals(Collections.emptyList(), dashboard.getReglasActivas());
                })
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

        when(auditStreamService.getAuditStream()).thenReturn(Flux.just(event));

        StepVerifier.create(bffController.getAuditStream())
                .expectNext(event)
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
