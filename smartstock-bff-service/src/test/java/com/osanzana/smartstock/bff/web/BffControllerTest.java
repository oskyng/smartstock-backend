package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.DashboardResponseDTO;
import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    void getDashboard_Success() {
        when(client.listarProductos(anyString(), anyLong())).thenReturn(Mono.just("[]"));
        when(client.listarLotes(anyString(), anyLong())).thenReturn(Mono.just("[]"));
        when(client.listarAlertas(anyString(), anyLong())).thenReturn(Mono.just("[]"));
        when(client.listarReglas(anyString(), anyLong())).thenReturn(Mono.just("[]"));

        StepVerifier.create(bffController.getDashboard("Bearer token", 1L))
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
        when(client.listarAlertas(anyString(), anyLong())).thenReturn(Mono.just("[]"));

        StepVerifier.create(bffController.listarAlertas("Bearer token", 1L))
                .expectNext("[]")
                .verifyComplete();
    }

    @Test
    void atenderAlerta_Success() {
        when(client.atenderAlerta(anyString(), anyLong())).thenReturn(Mono.empty());

        StepVerifier.create(bffController.atenderAlerta("Bearer token", 1L))
                .assertNext(response -> {
                    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
                })
                .verifyComplete();
    }

    @Test
    void listarProductos_Success() {
        when(client.listarProductos(anyString(), anyLong())).thenReturn(Mono.just("[]"));

        StepVerifier.create(bffController.listarProductos("Bearer token", 1L))
                .expectNext("[]")
                .verifyComplete();
    }
}
