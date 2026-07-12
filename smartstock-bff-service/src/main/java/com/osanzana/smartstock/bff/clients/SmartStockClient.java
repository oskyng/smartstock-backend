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
    private final WebClient commerceWebClient;
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

    // Métodos para Usuarios
    public Mono<Object> crearUsuario(Object usuarioRequest, Long comercioId) {
        return authWebClient.post()
                .uri("/api/v1/usuarios")
                .header("X-Comercio-ID", comercioId != null ? String.valueOf(comercioId) : "")
                .bodyValue(usuarioRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Métodos para Comercio
    public Mono<Object> listarComercios() {
        return commerceWebClient.get()
                .uri("/api/v1/comercios")
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> obtenerComercio(Long id) {
        return commerceWebClient.get()
                .uri("/api/v1/comercios/{id}", id)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> crearComercio(Object comercioRequest) {
        return commerceWebClient.post()
                .uri("/api/v1/comercios")
                .bodyValue(comercioRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> actualizarComercio(Long id, Object comercioRequest) {
        return commerceWebClient.put()
                .uri("/api/v1/comercios/{id}", id)
                .bodyValue(comercioRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Void> eliminarComercio(Long id) {
        return commerceWebClient.delete()
                .uri("/api/v1/comercios/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Métodos para Inventario
    public Mono<Object> listarProductos(Long comercioId) {
        return inventoryWebClient.get()
                .uri("/api/v1/productos")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> obtenerProducto(Long id) {
        return inventoryWebClient.get()
                .uri("/api/v1/productos/{id}", id)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> crearProducto(Long comercioId, Object productoRequest) {
        return inventoryWebClient.post()
                .uri("/api/v1/productos")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(productoRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> listarLotes(Long comercioId) {
        return inventoryWebClient.get()
                .uri("/api/v1/inventario/lotes")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> crearLote(Long comercioId, Object loteRequest) {
        return inventoryWebClient.post()
                .uri("/api/v1/inventario/lotes")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(loteRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    // Métodos para Finanzas
    public Mono<Object> listarReglas(Long comercioId) {
        return financeWebClient.get()
                .uri("/api/v1/reglas-depreciacion")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Object> crearRegla(Long comercioId, Object reglaRequest) {
        return financeWebClient.post()
                .uri("/api/v1/reglas-depreciacion")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(reglaRequest)
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Void> eliminarRegla(Long id) {
        return financeWebClient.delete()
                .uri("/api/v1/reglas-depreciacion/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Métodos para Alertas
    public Mono<Object> listarAlertas(Long comercioId) {
        return alertWebClient.get()
                .uri("/api/v1/alertas/pendientes")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(Object.class);
    }

    public Mono<Void> atenderAlerta(Long alertaId) {
        return alertWebClient.put()
                .uri("/api/v1/alertas/{id}/atender", alertaId)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
