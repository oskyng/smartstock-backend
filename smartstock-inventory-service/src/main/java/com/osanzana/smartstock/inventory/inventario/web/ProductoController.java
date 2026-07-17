package com.osanzana.smartstock.inventory.inventario.web;

import com.osanzana.smartstock.inventory.shared.dto.request.ProductoRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProductoResponseDTO;
import com.osanzana.smartstock.inventory.inventario.services.ProductoService;
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
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Tag(name = "Inventario - Productos", description = "Endpoints para la gestión de productos")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService productoService;

    /** GERENTE_TIENDA también llega aquí indirectamente vía GET /bff/dashboard (capital en riesgo). */
    @Operation(summary = "Listar todos los productos del comercio")
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERADOR_INVENTARIO', 'GERENTE_TIENDA')")
    public ResponseEntity<List<ProductoResponseDTO>> listar(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.ok(productoService.listarPorComercio(comercioId));
    }

    @Operation(summary = "Obtener producto por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public ResponseEntity<ProductoResponseDTO> obtener(
            @PathVariable Long id,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.ok(productoService.obtenerPorId(id, comercioId));
    }

    @Operation(summary = "Crear nuevo producto")
    @PostMapping
    @PreAuthorize("hasRole('OPERADOR_INVENTARIO')")
    public ResponseEntity<ProductoResponseDTO> crear(
            @Valid @RequestBody ProductoRequestDTO dto,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.guardar(dto, comercioId));
    }
}
