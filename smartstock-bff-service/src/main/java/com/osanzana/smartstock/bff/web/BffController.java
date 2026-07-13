package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.*;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
@Tag(name = "BFF Controller", description = "Endpoints para el Frontend (BFF)")
public class BffController {

    private final SmartStockClient client;
    private final AuditStreamService auditStreamService;

    @Operation(summary = "Stream de auditoría en tiempo real (SSE)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/audit/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertaEscaladaEvent>> getAuditStream() {
        return auditStreamService.getAuditStream();
    }

    @Operation(summary = "Login centralizado")
    @PostMapping("/auth/login")
    public Mono<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        return client.login(loginRequest);
    }

    // --- USUARIOS ---
    @Operation(summary = "Crear usuario", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/usuarios")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<UsuarioCreateResponseDTO> crearUsuario(
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId,
            @Valid @RequestBody UsuarioRequestDTO usuarioRequest) {
        return client.crearUsuario(usuarioRequest, comercioId);
    }

    @Operation(summary = "Listar usuarios del comercio (para actualizar o eliminar)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/usuarios")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<List<UsuarioResponseDTO>>> listarUsuarios(
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId) {
        return client.listarUsuarios(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Actualizar usuario", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<UsuarioResponseDTO>> actualizarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId,
            @Valid @RequestBody UsuarioUpdateRequestDTO usuarioRequest) {
        return client.actualizarUsuario(id, comercioId, usuarioRequest)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Desactivar usuario (borrado lógico)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<Void>> eliminarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId) {
        return client.eliminarUsuario(id, comercioId)
                .map(v -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Listar catálogo de roles (para el selector de creación de usuarios)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<List<RolResponseDTO>>> listarRoles() {
        return client.listarRoles()
                .map(ResponseEntity::ok);
    }

    // --- COMERCIOS ---
    @Operation(summary = "Listar comercios", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/comercios")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<ResponseEntity<List<ComercioResponseDTO>>> listarComercios() {
        return client.listarComercios()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtener comercio por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/comercios/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<ResponseEntity<ComercioResponseDTO>> obtenerComercio(@PathVariable Long id) {
        return client.obtenerComercio(id)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/comercios")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<ResponseEntity<ComercioResponseDTO>> crearComercio(@Valid @RequestBody ComercioRequestDTO comercioRequest) {
        return client.crearComercio(comercioRequest)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    @Operation(summary = "Actualizar comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/comercios/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<ResponseEntity<ComercioResponseDTO>> actualizarComercio(
            @PathVariable Long id,
            @Valid @RequestBody ComercioRequestDTO comercioRequest) {
        return client.actualizarComercio(id, comercioRequest)
                .map(ResponseEntity::ok);
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
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public Mono<DashboardResponseDTO> getDashboard(@RequestHeader("X-Comercio-ID") Long comercioId) {

        return Mono.zip(
                client.listarProductos(comercioId).onErrorReturn(Collections.emptyList()),
                client.listarLotes(comercioId).onErrorReturn(Collections.emptyList()),
                client.listarAlertas(comercioId).onErrorReturn(Collections.emptyList()),
                client.listarReglas(comercioId).onErrorReturn(Collections.emptyList())
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
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public Mono<ResponseEntity<List<ProductoResponseDTO>>> listarProductos(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarProductos(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtener producto por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/productos/{id}")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public Mono<ResponseEntity<ProductoResponseDTO>> obtenerProducto(@PathVariable Long id) {
        return client.obtenerProducto(id)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear producto", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/productos")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public Mono<ResponseEntity<ProductoResponseDTO>> crearProducto(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @Valid @RequestBody ProductoRequestDTO productoRequest) {
        return client.crearProducto(comercioId, productoRequest)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    // --- CATEGORÍAS ---
    @Operation(summary = "Listar categorías del comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/categorias")
    @PreAuthorize("hasAnyRole('OPERADOR_INVENTARIO', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<List<CategoriaResponseDTO>>> listarCategorias(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarCategorias(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear categoría", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/categorias")
    @PreAuthorize("hasAnyRole('OPERADOR_INVENTARIO', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<CategoriaResponseDTO>> crearCategoria(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @Valid @RequestBody CategoriaRequestDTO categoriaRequest) {
        return client.crearCategoria(comercioId, categoriaRequest)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    // --- PROVEEDORES ---
    @Operation(summary = "Listar proveedores del comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/proveedores")
    @PreAuthorize("hasAnyRole('OPERADOR_INVENTARIO', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<List<ProveedorResponseDTO>>> listarProveedores(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarProveedores(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear proveedor", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/proveedores")
    @PreAuthorize("hasAnyRole('OPERADOR_INVENTARIO', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<ProveedorResponseDTO>> crearProveedor(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @Valid @RequestBody ProveedorRequestDTO proveedorRequest) {
        return client.crearProveedor(comercioId, proveedorRequest)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    // --- LOTES ---
    @Operation(summary = "Listar lotes", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/inventario/lotes")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public Mono<ResponseEntity<List<LoteResponseDTO>>> listarLotes(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarLotes(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear lote", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/inventario/lotes")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public Mono<ResponseEntity<LoteResponseDTO>> crearLote(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @Valid @RequestBody LoteRequestDTO loteRequest) {
        return client.crearLote(comercioId, loteRequest)
                .map(res -> ResponseEntity.status(HttpStatus.CREATED).body(res));
    }

    // --- REGLAS DE DEPRECIACIÓN ---
    @Operation(summary = "Listar reglas de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/reglas-depreciacion")
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public Mono<ResponseEntity<List<ReglaDepreciacionResponseDTO>>> listarReglas(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarReglas(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Crear regla de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/reglas-depreciacion")
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public Mono<ResponseEntity<ReglaDepreciacionResponseDTO>> crearRegla(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @Valid @RequestBody ReglaDepreciacionRequestDTO reglaRequest) {
        return client.crearRegla(comercioId, reglaRequest)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Eliminar regla de depreciación", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/reglas-depreciacion/{id}")
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public Mono<ResponseEntity<Void>> eliminarRegla(@PathVariable Long id) {
        return client.eliminarRegla(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    // --- ALERTAS ---
    @Operation(summary = "Listar alertas", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/alertas")
    @PreAuthorize("hasRole('REPONEDOR_SALA')")
    public Mono<ResponseEntity<List<AlertaResponseDTO>>> listarAlertas(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarAlertas(comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Atender alerta (CA-07)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/alertas/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public Mono<ResponseEntity<Void>> atenderAlerta(@PathVariable Long id) {
        return client.atenderAlerta(id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Operation(summary = "Auditoría de alertas del comercio (panel de control del GERENTE_TIENDA)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/alertas/auditoria")
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public Mono<ResponseEntity<List<AlertaAuditoriaResponseDTO>>> listarAuditoriaAlertas(@RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarAuditoriaAlertas(comercioId)
                .map(ResponseEntity::ok);
    }
}
