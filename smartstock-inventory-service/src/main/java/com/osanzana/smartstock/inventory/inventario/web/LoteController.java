package com.osanzana.smartstock.inventory.inventario.web;

import com.osanzana.smartstock.inventory.shared.dto.request.LoteRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.LoteResponseDTO;
import com.osanzana.smartstock.inventory.inventario.services.LoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventario/lotes")
@RequiredArgsConstructor
@Tag(name = "Inventario - Lotes", description = "Endpoints para la gestión de lotes físicos")
@SecurityRequirement(name = "bearerAuth")
public class LoteController {

    private final LoteService loteService;

    @Operation(summary = "Registrar un nuevo lote")
    @PostMapping
    public ResponseEntity<LoteResponseDTO> crearLote(
            @RequestHeader("X-Comercio-ID") Long comercioId,
            @Valid @RequestBody LoteRequestDTO loteDto) {
        
        LoteResponseDTO nuevoLote = loteService.guardarLote(loteDto, comercioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLote);
    }

    @Operation(summary = "Listar lotes del comercio")
    @GetMapping
    public ResponseEntity<List<LoteResponseDTO>> listarLotes(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        
        List<LoteResponseDTO> lotes = loteService.listarPorComercio(comercioId);
        return ResponseEntity.ok(lotes);
    }
}
