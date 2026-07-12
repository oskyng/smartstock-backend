package com.osanzana.smartstock.alert.alertas.web;

import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Endpoints para la gestión de tareas de reetiquetado")
@SecurityRequirement(name = "bearerAuth")
public class AlertaController {

    private final AlertaService alertaService;

    @Operation(summary = "Listar alertas pendientes")
    @GetMapping("/pendientes")
    public ResponseEntity<List<AlertaResponseDTO>> listarPendientes(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        
        List<AlertaResponseDTO> alertas = alertaService.listarPendientesPorComercio(comercioId);
        return ResponseEntity.ok(alertas);
    }

    @Operation(summary = "Marcar alerta como atendida")
    @PutMapping("/{id}/atender")
    public ResponseEntity<Void> atenderAlerta(@PathVariable Long id) {
        alertaService.atenderAlerta(id);
        return ResponseEntity.noContent().build();
    }
}
