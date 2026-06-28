package com.osanzana.smartstock.bff.clients;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmartStockClient {

    private final WebClient authWebClient;
    private final WebClient inventoryWebClient;
    private final WebClient financeWebClient;
    private final WebClient alertWebClient;

    // Métodos para Auth
    public Mono<Object> login(Object loginRequest) {
        return authWebClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Métodos para Inventario
    public Mono<Object> listarProductos(String token, Long comercioId) {
        return inventoryWebClient.get()
                .uri("/api/v1/productos")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> listarLotes(String token, Long comercioId) {
        return inventoryWebClient.get()
                .uri("/api/v1/inventario/lotes")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Métodos para Finanzas
    public Mono<Object> listarReglas(String token, Long comercioId) {
        return financeWebClient.get()
                .uri("/api/v1/reglas-depreciacion")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Métodos para Alertas
    public Mono<Object> listarAlertas(String token, Long comercioId) {
        return alertWebClient.get()
                .uri("/api/v1/alertas")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Void> atenderAlerta(String token, Long alertaId) {
        return alertWebClient.patch()
                .uri("/api/v1/alertas/{id}/atender", alertaId)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
