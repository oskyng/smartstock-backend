package com.osanzana.smartstock.auth.autenticacion.web;

import com.osanzana.smartstock.auth.autenticacion.services.UsuarioService;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioCreateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Crear Admin de Sistema", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN_SISTEMA')")
    public ResponseEntity<UsuarioResponseDTO> crearAdminSistema(@Valid @RequestBody UsuarioCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearAdminSistema(request));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario", description = "Permite a ADMIN_SISTEMA crear cualquier rol, y a GERENTE_TIENDA crear roles operativos.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> crearUsuario(
            @Valid @RequestBody UsuarioRequestDTO request,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authentication) {

        String emailSolicitante = authentication.getName();
        Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario solicitante no encontrado."));

        String rolSolicitante = solicitante.getRol().getNombre();
        Long idComercioContexto = solicitante.getComercio() != null ? solicitante.getComercio().getId() : null;

        Long idComercioOperacion = (comercioHeader != null) ? comercioHeader : idComercioContexto;

        UsuarioResponseDTO responseDTO = usuarioService.crearUsuario(request, idComercioOperacion, rolSolicitante);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Usuario creado exitosamente.");
        response.put("usuario", responseDTO);
        response.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
