package com.osanzana.smartstock.inventory.inventario.web;

import com.osanzana.smartstock.inventory.shared.dto.request.ProveedorRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProveedorResponseDTO;
import com.osanzana.smartstock.inventory.inventario.services.ProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Inventario - Proveedores", description = "Endpoints para la gestión de proveedores")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('OPERADOR_INVENTARIO', 'GERENTE_TIENDA')")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @Operation(summary = "Listar proveedores del comercio")
    @GetMapping
    public ResponseEntity<List<ProveedorResponseDTO>> listar(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.ok(proveedorService.listarPorComercio(comercioId));
    }

    @Operation(summary = "Crear nuevo proveedor")
    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> crear(
            @Valid @RequestBody ProveedorRequestDTO dto,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(dto, comercioId));
    }
}
