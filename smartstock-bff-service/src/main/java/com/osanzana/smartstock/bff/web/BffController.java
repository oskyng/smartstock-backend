package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.*;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
@Tag(name = "BFF Controller", description = "Endpoints para el Frontend (BFF)")
public class BffController {

    private final SmartStockClient client;
    private final AuditStreamService auditStreamService;

    @Operation(summary = "Stream de auditoría en tiempo real (SSE)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/audit/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Flux<AlertaEscaladaEvent> getAuditStream() {
        return auditStreamService.getAuditStream();
    }

    @Operation(summary = "Login centralizado")
    @PostMapping("/auth/login")
    public Mono<LoginResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        return client.login(loginRequest);
    }

    // --- USUARIOS ---
    @Operation(summary = "Crear usuario", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/usuarios")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> crearUsuario(
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId,
            @RequestBody Object usuarioRequest) {
        return client.crearUsuario(usuarioRequest, comercioId);
    }

    // --- COMERCIOS ---
    @Operation(summary = "Listar comercios", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/comercios")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<Object> listarComercios() {
        return client.listarComercios();
    }

    @Operation(summary = "Obtener comercio por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/comercios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> obtenerComercio(@PathVariable Long id) {
        return client.obtenerComercio(id);
    }

    @Operation(summary = "Crear comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/comercios")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<Object> crearComercio(@RequestBody Object comercioRequest) {
        return client.crearComercio(comercioRequest);
    }

    @Operation(summary = "Actualizar comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/comercios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> actualizarComercio(
            @PathVariable Long id,
            @RequestBody Object comercioRequest) {
        return client.actualizarComercio(id, comercioRequest);
    }

    @Operation(summary = "Eliminar comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/comercios/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<ResponseEntity<Void>> eliminarComercio(@PathVariable Long id) {
        return client.eliminarComercio(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Operation(summary = "Obtener Dashboard consolidado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<DashboardResponseDTO> getDashboard(@RequestHeader("X-Comercio-ID") Long comercioId) {
        
        return Mono.zip(
                client.listarProductos(comercioId).onErrorReturn("[]"),
                client.listarLotes(comercioId).onErrorReturn("[]"),
                client.listarAlertas(comercioId).onErrorReturn("[]"),
                client.listarReglas(comercioId).onErrorReturn("[]")
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
    public Mono<Object> listarProductos(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarProductos(comercioId);
    }

    @Operation(summary = "Obtener producto por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/productos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> obtenerProducto(@PathVariable Long id) {
        return client.obtenerProducto(id);
    }

    @Operation(summary = "Crear producto", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> crearProducto(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @RequestBody Object productoRequest) {
        return client.crearProducto(comercioId, productoRequest);
    }

    // --- LOTES ---
    @Operation(summary = "Listar lotes", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/inventario/lotes")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> listarLotes(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarLotes(comercioId);
    }

    @Operation(summary = "Crear lote", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/inventario/lotes")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> crearLote(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @RequestBody Object loteRequest) {
        return client.crearLote(comercioId, loteRequest);
    }

    // --- REGLAS DE DEPRECIACIÓN ---
    @Operation(summary = "Listar reglas de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/reglas-depreciacion")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> listarReglas(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarReglas(comercioId);
    }

    @Operation(summary = "Crear regla de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/reglas-depreciacion")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> crearRegla(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @RequestBody Object reglaRequest) {
        return client.crearRegla(comercioId, reglaRequest);
    }

    @Operation(summary = "Eliminar regla de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/reglas-depreciacion/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<Void>> eliminarRegla(@PathVariable Long id) {
        return client.eliminarRegla(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    // --- ALERTAS ---
    @Operation(summary = "Listar alertas", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/alertas")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<Object> listarAlertas(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarAlertas(comercioId);
    }

    @Operation(summary = "Atender alerta", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/alertas/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<ResponseEntity<Void>> atenderAlerta(@PathVariable Long id) {
        return client.atenderAlerta(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
}
