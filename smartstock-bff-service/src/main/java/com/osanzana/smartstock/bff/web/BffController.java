package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.*;
import com.osanzana.smartstock.bff.security.JwtUtils;
import com.osanzana.smartstock.bff.service.InfraDiagService;
import com.osanzana.smartstock.bff.stream.AuditStreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bff")
@RequiredArgsConstructor
@Tag(name = "BFF Controller", description = "Endpoints para el Frontend (BFF)")
public class BffController {

    private final SmartStockClient client;
    private final AuditStreamService auditStreamService;
    private final InfraDiagService infraDiagService;

    @Value("${smartstock.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * false por defecto porque el stack local (docker-compose, ng serve) corre sobre HTTP plano;
     * una cookie Secure jamás se enviaría de vuelta y rompería el login. Debe activarse vía
     * SMARTSTOCK_COOKIE_SECURE=true en cualquier despliegue real servido con HTTPS.
     */
    @Value("${smartstock.cookie.secure:false}")
    private boolean cookieSecure;

    @Operation(summary = "Stream de auditoría en tiempo real (SSE)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping(value = "/audit/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public Flux<ServerSentEvent<AlertaEscaladaEvent>> getAuditStream(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return auditStreamService.getAuditStream(comercioId);
    }

    /**
     * El JWT viaja únicamente por cookie httpOnly (nunca en el body): así un XSS no puede leerlo
     * desde JavaScript. SameSite=Strict basta como protección CSRF porque el navegador jamás habla
     * directamente con los microservicios internos, solo con este bff, siempre same-origin
     * (nginx/ng-serve hacen de proxy same-origin en todos los entornos de este proyecto).
     */
    @Operation(summary = "Login centralizado")
    @PostMapping("/auth/login")
    public Mono<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest, HttpServletResponse response) {
        return client.login(loginRequest)
                .map(loginResponse -> {
                    ResponseCookie cookie = ResponseCookie.from(JwtUtils.AUTH_COOKIE_NAME,loginResponse.getToken())
                            .httpOnly(true)
                            .secure(cookieSecure)
                            .sameSite("Strict")
                            .path("/")
                            .maxAge(Duration.ofMillis(jwtExpirationMs))
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    loginResponse.setToken(null);
                    return loginResponse;
                });
    }

    @Operation(summary = "Cerrar sesión: invalida la cookie de autenticación")
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(JwtUtils.AUTH_COOKIE_NAME,"")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.noContent().build();
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

    @Operation(summary = "Suspender usuario (borrado lógico, reversible con /reactivar)", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<Void>> eliminarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId) {
        return client.eliminarUsuario(id, comercioId)
                .map(v -> ResponseEntity.noContent().<Void>build())
                .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Reactivar usuario suspendido", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/usuarios/{id}/reactivar")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<UsuarioResponseDTO>> reactivarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId) {
        return client.reactivarUsuario(id, comercioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Restablecer la contraseña de un usuario (acción administrativa)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/usuarios/{id}/password")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<ResponseEntity<Void>> cambiarContrasenaUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId,
            @Valid @RequestBody CambiarContrasenaRequestDTO request) {
        return client.cambiarContrasenaUsuario(id, comercioId, request)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
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
                client.listarLotesInterno(comercioId).onErrorReturn(Collections.emptyList()),
                client.listarAlertas(comercioId).onErrorReturn(Collections.emptyList()),
                client.listarReglas(comercioId).onErrorReturn(Collections.emptyList())
        ).map(tuple -> DashboardResponseDTO.builder()
                .productos(tuple.getT1())
                .lotesRecientes(tuple.getT2())
                .alertasPendientes(tuple.getT3())
                .reglasActivas(tuple.getT4())
                .capitalEnRiesgo(calcularCapitalEnRiesgo(tuple.getT2(), tuple.getT4()))
                .build());
    }

    private BigDecimal calcularCapitalEnRiesgo(List<LoteResponseDTO> lotes, List<ReglaDepreciacionResponseDTO> reglas) {
        LocalDate hoy = LocalDate.now();
        BigDecimal total = BigDecimal.ZERO;

        for (LoteResponseDTO lote : lotes) {
            if (lote.getCostoUnitario() == null || lote.getCantidadActual() == null || lote.getFechaVencimiento() == null) {
                continue;
            }
            long diasParaVencer = ChronoUnit.DAYS.between(hoy, lote.getFechaVencimiento());

            boolean enRiesgo = reglas.stream()
                    .filter(r -> r.getActiva() != null && r.getActiva() == 1)
                    .filter(r -> r.getNombreCategoria() != null && r.getNombreCategoria().equals(lote.getNombreCategoria()))
                    .anyMatch(r -> r.getDiasCriticosMin() != null && diasParaVencer <= r.getDiasCriticosMin());

            if (enRiesgo) {
                total = total.add(lote.getCostoUnitario().multiply(BigDecimal.valueOf(lote.getCantidadActual())));
            }
        }

        return total;
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

    // --- INFRAESTRUCTURA ---
    @Operation(summary = "Estado de infraestructura: salud de microservicios y metadata real de Kafka", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/infra/status")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public Mono<InfraStatusDTO> getInfraStatus() {
        return infraDiagService.obtenerEstadoInfraestructura();
    }
}
