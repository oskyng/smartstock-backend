package com.osanzana.smartstock.bff.web;

import com.osanzana.smartstock.bff.clients.SmartStockClient;
import com.osanzana.smartstock.bff.dto.DashboardResponseDTO;
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
    public Mono<Object> login(@RequestBody Object loginRequest) {
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
    @PreAuthorize("hasRole('REPONEDOR_SALA')")
    public Mono<ResponseEntity<Void>> atenderAlerta(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        return client.atenderAlerta(token, id)
                .then(Mono.just(ResponseEntity.noContent().build()));
    }
    
    @Operation(summary = "Listar productos", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/productos")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public Mono<Object> listarProductos(
            @RequestHeader("Authorization") String token,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return client.listarProductos(token, comercioId);
    }
}
