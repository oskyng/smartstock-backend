package com.osanzana.smartstock.finance.finanzas.web;

import com.osanzana.smartstock.finance.shared.dto.request.ReglaDepreciacionRequestDTO;
import com.osanzana.smartstock.finance.shared.dto.response.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.finance.finanzas.services.ReglaDepreciacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reglas-depreciacion")
@RequiredArgsConstructor
@Tag(name = "Finanzas - Reglas de Depreciación", description = "Endpoints para la gestión de reglas de precios dinámicos")
@SecurityRequirement(name = "bearerAuth")
public class ReglaDepreciacionController {

    private final ReglaDepreciacionService reglaService;

    @Operation(summary = "Listar todas las reglas del comercio")
    @GetMapping
    public ResponseEntity<List<ReglaDepreciacionResponseDTO>> listar(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.ok(reglaService.listarPorComercio(comercioId));
    }

    @Operation(summary = "Crear nueva regla")
    @PostMapping
    public ResponseEntity<ReglaDepreciacionResponseDTO> crear(
            @Valid @RequestBody ReglaDepreciacionRequestDTO dto,
            @RequestHeader("X-Comercio-ID") Long comercioId,
            java.security.Principal principal) {
        String emailGerente = principal.getName();
        return ResponseEntity.status(HttpStatus.CREATED).body(reglaService.guardar(dto, comercioId, emailGerente));
    }

    @Operation(summary = "Eliminar regla")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reglaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
