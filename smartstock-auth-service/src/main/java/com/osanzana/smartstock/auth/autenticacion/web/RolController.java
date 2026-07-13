package com.osanzana.smartstock.auth.autenticacion.web;

import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.repositories.RolRepository;
import com.osanzana.smartstock.auth.shared.dto.response.RolResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Catálogo de roles disponibles en el sistema")
public class RolController {

    private final RolRepository rolRepository;

    @Operation(summary = "Listar catálogo de roles", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<List<RolResponseDTO>> listar() {
        List<RolResponseDTO> roles = rolRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .toList();
        return ResponseEntity.ok(roles);
    }

    private RolResponseDTO mapToResponseDTO(Rol rol) {
        return RolResponseDTO.builder()
                .id(rol.getId())
                .nombre(rol.getNombre())
                .descripcion(rol.getDescripcion())
                .build();
    }
}
