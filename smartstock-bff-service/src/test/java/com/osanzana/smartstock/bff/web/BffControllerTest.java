package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.DashboardResponseDTO;
import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

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
        Object usuarioRequest = new Object();
        when(client.crearUsuario(any(), anyLong())).thenReturn(Mono.just("Usuario creado"));

        StepVerifier.create(bffController.crearUsuario(1L, usuarioRequest))
                .expectNext("Usuario creado")
                .verifyComplete();
    }

    @Test
    void getDashboard_Success() {
        when(client.listarProductos(anyLong())).thenReturn(Mono.just("[]"));
        when(client.listarLotes(anyLong())).thenReturn(Mono.just("[]"));
        when(client.listarAlertas(anyLong())).thenReturn(Mono.just("[]"));
        when(client.listarReglas(anyLong())).thenReturn(Mono.just("[]"));

        StepVerifier.create(bffController.getDashboard(1L))
                .assertNext(dashboard -> {
                    assertEquals("[]", dashboard.getProductos());
                    assertEquals("[]", dashboard.getLotesRecientes());
                    assertEquals("[]", dashboard.getAlertasPendientes());
                    assertEquals("[]", dashboard.getReglasActivas());
                })
                .verifyComplete();
    }

    @Test
    void listarAlertas_Success() {
        Long comercioId = 1L;
        when(client.listarAlertas(eq(comercioId))).thenReturn(Mono.just("[]"));

        StepVerifier.create(bffController.listarAlertas(comercioId))
                .expectNext("[]")
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
        when(client.listarProductos(anyLong())).thenReturn(Mono.just("[]"));

        StepVerifier.create(bffController.listarProductos(1L))
                .expectNext("[]")
                .verifyComplete();
    }
}
