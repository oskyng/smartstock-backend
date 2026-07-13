package com.osanzana.smartstock.inventory.inventario.web;

import com.osanzana.smartstock.inventory.shared.dto.request.CategoriaRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.CategoriaResponseDTO;
import com.osanzana.smartstock.inventory.inventario.services.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Inventario - Categorías", description = "Endpoints para la gestión de categorías de producto")
@SecurityRequirement(name = "bearerAuth")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @Operation(summary = "Listar categorías del comercio")
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> listar(
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.ok(categoriaService.listarPorComercio(comercioId));
    }

    @Operation(summary = "Crear nueva categoría")
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crear(
            @Valid @RequestBody CategoriaRequestDTO dto,
            @RequestHeader("X-Comercio-ID") Long comercioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crear(dto, comercioId));
    }
}
