package com.osanzana.smartstock.alert.alertas.web;

import com.osanzana.smartstock.alert.shared.dto.response.AlertaAuditoriaResponseDTO;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Endpoints para la gestión de tareas de reetiquetado")
@SecurityRequirement(name = "bearerAuth")
public class AlertaController {

    private final AlertaService alertaService;

    /** GERENTE_TIENDA también llega aquí indirectamente vía GET /bff/dashboard (alertasPendientes). */
    @Operation(summary = "Listar alertas pendientes")
    @GetMapping("/pendientes")
    @PreAuthorize("hasAnyRole('REPONEDOR_SALA', 'GERENTE_TIENDA')")
    public ResponseEntity<List<AlertaResponseDTO>> listarPendientes(
            @RequestHeader("X-Comercio-ID") Long comercioId) {

        List<AlertaResponseDTO> alertas = alertaService.listarPendientesPorComercio(comercioId);
        return ResponseEntity.ok(alertas);
    }

    @Operation(summary = "Listar auditoría completa de alertas del comercio (todos los estados, para el panel de control del GERENTE_TIENDA)")
    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('GERENTE_TIENDA')")
    public ResponseEntity<List<AlertaAuditoriaResponseDTO>> listarAuditoria(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.ok(alertaService.listarAuditoriaPorComercio(comercioId));
    }

    /**
     * X-Comercio-ID es opcional aquí porque ADMIN_SISTEMA (único rol exento de la validación
     * multi-tenant del JwtAuthenticationFilter) puede operar sin ese header; para el resto de los
     * roles el filtro ya garantiza que llegue presente antes de alcanzar este método.
     */
    @Operation(summary = "Marcar alerta como atendida")
    @PutMapping("/{id}/atender")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA', 'REPONEDOR_SALA')")
    public ResponseEntity<Void> atenderAlerta(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioId) {
        alertaService.atenderAlerta(id, comercioId);
        return ResponseEntity.noContent().build();
    }
}
