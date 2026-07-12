package com.osanzana.smartstock.bff.clients;

import com.osanzana.smartstock.bff.dto.AlertaResponseDTO;
import com.osanzana.smartstock.bff.dto.ComercioRequestDTO;
import com.osanzana.smartstock.bff.dto.ComercioResponseDTO;
import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
import com.osanzana.smartstock.bff.dto.LoteRequestDTO;
import com.osanzana.smartstock.bff.dto.LoteResponseDTO;
import com.osanzana.smartstock.bff.dto.ProductoRequestDTO;
import com.osanzana.smartstock.bff.dto.ProductoResponseDTO;
import com.osanzana.smartstock.bff.dto.ReglaDepreciacionRequestDTO;
import com.osanzana.smartstock.bff.dto.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.bff.dto.UsuarioCreateResponseDTO;
import com.osanzana.smartstock.bff.dto.UsuarioRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

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
    public Mono<UsuarioCreateResponseDTO> crearUsuario(UsuarioRequestDTO usuarioRequest, Long comercioId) {
        return authWebClient.post()
                .uri("/api/v1/usuarios")
                .header("X-Comercio-ID", comercioId != null ? String.valueOf(comercioId) : "")
                .bodyValue(usuarioRequest)
                .retrieve()
                .bodyToMono(UsuarioCreateResponseDTO.class);
    }

    // Métodos para Comercio
    public Mono<List<ComercioResponseDTO>> listarComercios() {
        return commerceWebClient.get()
                .uri("/api/v1/comercios")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ComercioResponseDTO>>() {});
    }

    public Mono<ComercioResponseDTO> obtenerComercio(Long id) {
        return commerceWebClient.get()
                .uri("/api/v1/comercios/{id}", id)
                .retrieve()
                .bodyToMono(ComercioResponseDTO.class);
    }

    public Mono<ComercioResponseDTO> crearComercio(ComercioRequestDTO comercioRequest) {
        return commerceWebClient.post()
                .uri("/api/v1/comercios")
                .bodyValue(comercioRequest)
                .retrieve()
                .bodyToMono(ComercioResponseDTO.class);
    }

    public Mono<ComercioResponseDTO> actualizarComercio(Long id, ComercioRequestDTO comercioRequest) {
        return commerceWebClient.put()
                .uri("/api/v1/comercios/{id}", id)
                .bodyValue(comercioRequest)
                .retrieve()
                .bodyToMono(ComercioResponseDTO.class);
    }

    public Mono<Void> eliminarComercio(Long id) {
        return commerceWebClient.delete()
                .uri("/api/v1/comercios/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Métodos para Inventario
    public Mono<List<ProductoResponseDTO>> listarProductos(Long comercioId) {
        return inventoryWebClient.get()
                .uri("/api/v1/productos")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ProductoResponseDTO>>() {});
    }

    public Mono<ProductoResponseDTO> obtenerProducto(Long id) {
        return inventoryWebClient.get()
                .uri("/api/v1/productos/{id}", id)
                .retrieve()
                .bodyToMono(ProductoResponseDTO.class);
    }

    public Mono<ProductoResponseDTO> crearProducto(Long comercioId, ProductoRequestDTO productoRequest) {
        return inventoryWebClient.post()
                .uri("/api/v1/productos")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(productoRequest)
                .retrieve()
                .bodyToMono(ProductoResponseDTO.class);
    }

    public Mono<List<LoteResponseDTO>> listarLotes(Long comercioId) {
        return inventoryWebClient.get()
                .uri("/api/v1/inventario/lotes")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<LoteResponseDTO>>() {});
    }

    public Mono<LoteResponseDTO> crearLote(Long comercioId, LoteRequestDTO loteRequest) {
        return inventoryWebClient.post()
                .uri("/api/v1/inventario/lotes")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(loteRequest)
                .retrieve()
                .bodyToMono(LoteResponseDTO.class);
    }

    // Métodos para Finanzas
    public Mono<List<ReglaDepreciacionResponseDTO>> listarReglas(Long comercioId) {
        return financeWebClient.get()
                .uri("/api/v1/reglas-depreciacion")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<ReglaDepreciacionResponseDTO>>() {});
    }

    public Mono<ReglaDepreciacionResponseDTO> crearRegla(Long comercioId, ReglaDepreciacionRequestDTO reglaRequest) {
        return financeWebClient.post()
                .uri("/api/v1/reglas-depreciacion")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .bodyValue(reglaRequest)
                .retrieve()
                .bodyToMono(ReglaDepreciacionResponseDTO.class);
    }

    public Mono<Void> eliminarRegla(Long id) {
        return financeWebClient.delete()
                .uri("/api/v1/reglas-depreciacion/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // Métodos para Alertas
    public Mono<List<AlertaResponseDTO>> listarAlertas(Long comercioId) {
        return alertWebClient.get()
                .uri("/api/v1/alertas/pendientes")
                .header("X-Comercio-ID", String.valueOf(comercioId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<AlertaResponseDTO>>() {});
    }

    public Mono<Void> atenderAlerta(Long alertaId) {
        return alertWebClient.put()
                .uri("/api/v1/alertas/{id}/atender", alertaId)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
