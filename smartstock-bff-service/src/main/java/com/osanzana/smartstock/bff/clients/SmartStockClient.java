package com.osanzana.smartstock.bff.clients;

import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
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
    public Mono<LoginResponseDTO> login(LoginRequestDTO loginRequest) {
        return authWebClient.post()
                .uri("/api/v1/auth/login")
                .bodyValue(loginRequest)
                .retrieve()
                .bodyToMono(LoginResponseDTO.class);
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

    public Mono<Object> obtenerProducto(String token, Long id) {
        return inventoryWebClient.get()
                .uri("/api/v1/productos/{id}", id)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> crearProducto(String token, Long comercioId, Object productoRequest) {
        return inventoryWebClient.post()
                .uri("/api/v1/productos")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(productoRequest)
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

    public Mono<Object> crearLote(String token, Long comercioId, Object loteRequest) {
        return inventoryWebClient.post()
                .uri("/api/v1/inventario/lotes")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(loteRequest)
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

    public Mono<Object> crearRegla(String token, Long comercioId, Object reglaRequest) {
        return financeWebClient.post()
                .uri("/api/v1/reglas-depreciacion")
                .header("Authorization", token)
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(reglaRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Void> eliminarRegla(String token, Long id) {
        return financeWebClient.delete()
                .uri("/api/v1/reglas-depreciacion/{id}", id)
                .header("Authorization", token)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Métodos para Alertas
    public Mono<Object> listarAlertas(String token, Long comercioId) {
        return alertWebClient.get()
                .uri("/api/v1/alertas/pendientes")
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
