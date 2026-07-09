package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.DashboardResponseDTO;
import com.osanzana.smartstock.bff.dto.LoginRequestDTO;
import com.osanzana.smartstock.bff.dto.LoginResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
@Tag(name = "BFF Controller", description = "Endpoints para el Frontend (BFF)")
public class BffController {

    private final SmartStockClient client;

    @Operation(summary = "Login centralizado")
    @PostMapping("/auth/login")
    public Mono<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        return client.login(loginRequest);
    }

    @Operation(summary = "Obtener Dashboard consolidado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<DashboardResponseDTO> getDashboard(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        
        return Mono.zip(
                client.listarProductos(token, comercioId).onErrorReturn("[]"),
                client.listarLotes(token, comercioId).onErrorReturn("[]"),
                client.listarAlertas(token, comercioId).onErrorReturn("[]"),
                client.listarReglas(token, comercioId).onErrorReturn("[]")
        ).map(tuple -> DashboardResponseDTO.builder()
                .productos(tuple.getT1())
                .lotesRecientes(tuple.getT2())
                .alertasPendientes(tuple.getT3())
                .reglasActivas(tuple.getT4())
                .build());
    }

    // --- PRODUCTOS ---
    @Operation(summary = "Listar productos", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> listarProductos(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarProductos(token, comercioId);
    }

    @Operation(summary = "Obtener producto por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> obtenerProducto(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        return client.obtenerProducto(token, id);
    }

    @Operation(summary = "Crear producto", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> crearProducto(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @RequestBody Object productoRequest) {
        return client.crearProducto(token, comercioId, productoRequest);
    }

    // --- LOTES ---
    @Operation(summary = "Listar lotes", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/inventario/lotes")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> listarLotes(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarLotes(token, comercioId);
    }

    @Operation(summary = "Crear lote", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/inventario/lotes")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> crearLote(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @RequestBody Object loteRequest) {
        return client.crearLote(token, comercioId, loteRequest);
    }

    // --- REGLAS DE DEPRECIACIÓN ---
    @Operation(summary = "Listar reglas de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/reglas-depreciacion")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> listarReglas(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarReglas(token, comercioId);
    }

    @Operation(summary = "Crear regla de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/reglas-depreciacion")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> crearRegla(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @RequestBody Object reglaRequest) {
        return client.crearRegla(token, comercioId, reglaRequest);
    }

    @Operation(summary = "Eliminar regla de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/reglas-depreciacion/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<Void>> eliminarRegla(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        return client.eliminarRegla(token, id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    // --- ALERTAS ---
    @Operation(summary = "Listar alertas", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> listarAlertas(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarAlertas(token, comercioId);
    }

    @Operation(summary = "Atender alerta", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/alertas/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<ResponseEntity<Void>> atenderAlerta(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        return client.atenderAlerta(token, id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
