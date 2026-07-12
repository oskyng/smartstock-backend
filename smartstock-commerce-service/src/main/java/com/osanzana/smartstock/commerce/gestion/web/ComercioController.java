package com.osanzana.smartstock.commerce.gestion.web;

import com.osanzana.smartstock.commerce.gestion.services.ComercioService;
import com.osanzana.smartstock.commerce.shared.dto.request.ComercioRequestDTO;
import com.osanzana.smartstock.commerce.shared.dto.response.ComercioResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/comercios")
@RequiredArgsConstructor
@Tag(name = "Comercio Controller", description = "Gestión de comercios (Multi-tenant)")
public class ComercioController {

    private final ComercioService comercioService;

    @Operation(summary = "Crear un nuevo comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<ComercioResponseDTO> crearComercio(@Valid @RequestBody ComercioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(comercioService.crearComercio(request));
    }

    @Operation(summary = "Listar todos los comercios", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<List<ComercioResponseDTO>> listarComercios() {
        return ResponseEntity.ok(comercioService.listarComercios());
    }

    @Operation(summary = "Obtener comercio por ID", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public ResponseEntity<ComercioResponseDTO> obtenerComercio(
            @PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String rol = auth.getAuthorities().iterator().next().getAuthority();
        
        // Extraer idComercio de los claims del token si es posible, o pasar null si es ADMIN
        Long idComercioContexto = null;
        if (auth.getDetails() instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            Object cid = details.get("idComercio");
            if (cid != null) {
                idComercioContexto = Long.valueOf(cid.toString());
            }
        }
        
        return ResponseEntity.ok(comercioService.obtenerPorId(id, rol, idComercioContexto));
    }

    @Operation(summary = "Actualizar un comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'GERENTE_TIENDA')")
    public ResponseEntity<ComercioResponseDTO> actualizarComercio(
            @PathVariable Long id,
            @Valid @RequestBody ComercioRequestDTO request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String rol = auth.getAuthorities().iterator().next().getAuthority();
        
        Long idComercioContexto = null;
        if (auth.getDetails() instanceof Map) {
            Map<String, Object> details = (Map<String, Object>) auth.getDetails();
            Object cid = details.get("idComercio");
            if (cid != null) {
                idComercioContexto = Long.valueOf(cid.toString());
            }
        }
        
        return ResponseEntity.ok(comercioService.updateComercio(id, request, rol, idComercioContexto));
    }

    @Operation(summary = "Eliminar un comercio", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<Void> eliminarComercio(@PathVariable Long id) {
        comercioService.deleteComercio(id);
        return ResponseEntity.noContent().build();
    }
}
