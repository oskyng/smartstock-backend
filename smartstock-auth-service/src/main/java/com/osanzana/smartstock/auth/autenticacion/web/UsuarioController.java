package com.osanzana.smartstock.auth.autenticacion.web;

import com.osanzana.smartstock.auth.autenticacion.services.UsuarioService;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.CambiarContrasenaRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioCreateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioUpdateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.auth.shared.exception.UnauthorizedActionException;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints para gestión de usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    @Operation(summary = "Crear Admin de Sistema")
    @PostMapping("/admin")
    public ResponseEntity<UsuarioResponseDTO> crearAdminSistema(@Valid @RequestBody UsuarioCreateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crearAdminSistema(request));
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario", description = "Permite a ADMIN_SISTEMA crear cualquier rol, y a GERENTE_TIENDA crear roles operativos.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> crearUsuario(
            @Valid @RequestBody UsuarioRequestDTO request,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authParam) {

        ContextoSolicitante contexto = resolverContexto(authParam, comercioHeader);

        UsuarioResponseDTO responseDTO = usuarioService.crearUsuario(request, contexto.idComercioOperacion, contexto.rolSolicitante);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Usuario creado exitosamente.");
        response.put("usuario", responseDTO);
        response.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios del comercio", description = "ADMIN_SISTEMA ve todos los usuarios (o los de un comercio si se indica X-Comercio-ID); GERENTE_TIENDA ve solo los de su propio comercio.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<UsuarioResponseDTO>> listarUsuarios(
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authParam) {

        ContextoSolicitante contexto = resolverContexto(authParam, comercioHeader);
        return ResponseEntity.ok(usuarioService.listar(contexto.idComercioOperacion, contexto.rolSolicitante));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario", description = "ADMIN_SISTEMA puede editar cualquier usuario; GERENTE_TIENDA solo usuarios OPERADOR_INVENTARIO/REPONEDOR_SALA de su propio comercio.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UsuarioResponseDTO> actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequestDTO request,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authParam) {

        ContextoSolicitante contexto = resolverContexto(authParam, comercioHeader);
        return ResponseEntity.ok(usuarioService.actualizar(id, request, contexto.idComercioOperacion, contexto.rolSolicitante));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Suspender un usuario", description = "Borrado lógico (activo=0), reversible con /reactivar. ADMIN_SISTEMA puede suspender cualquier usuario; GERENTE_TIENDA solo usuarios OPERADOR_INVENTARIO/REPONEDOR_SALA de su propio comercio.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> eliminarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authParam) {

        ContextoSolicitante contexto = resolverContexto(authParam, comercioHeader);
        usuarioService.eliminar(id, contexto.idComercioOperacion, contexto.rolSolicitante);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    @Operation(summary = "Reactivar un usuario suspendido", description = "Revierte la suspensión (activo=1). Mismo alcance que suspender.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UsuarioResponseDTO> reactivarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authParam) {

        ContextoSolicitante contexto = resolverContexto(authParam, comercioHeader);
        return ResponseEntity.ok(usuarioService.reactivar(id, contexto.idComercioOperacion, contexto.rolSolicitante));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Restablecer la contraseña de un usuario", description = "Acción administrativa: no requiere la contraseña anterior. Mismo alcance que suspender/editar.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> cambiarContrasena(
            @PathVariable Long id,
            @Valid @RequestBody CambiarContrasenaRequestDTO request,
            @RequestHeader(value = "X-Comercio-ID", required = false) Long comercioHeader,
            Authentication authParam) {

        ContextoSolicitante contexto = resolverContexto(authParam, comercioHeader);
        usuarioService.cambiarContrasena(id, request, contexto.idComercioOperacion, contexto.rolSolicitante);
        return ResponseEntity.noContent().build();
    }

    private ContextoSolicitante resolverContexto(Authentication authParam, Long comercioHeader) {
        if (authParam == null) {
            throw new UnauthorizedActionException("No se pudo resolver la identidad del solicitante.");
        }
        String emailSolicitante = authParam.getName();
        Usuario solicitante = usuarioRepository.findByEmail(emailSolicitante)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario solicitante no encontrado."));

        String rolSolicitante = solicitante.getRol().getNombre();
        Long idComercioContexto = solicitante.getComercio() != null ? solicitante.getComercio().getId() : null;

        // Blindaje Multi-tenant: Si no es ADMIN_SISTEMA, se ignora el header y se fuerza su propio comercio.
        Long idComercioOperacion;
        if ("ADMIN_SISTEMA".equals(rolSolicitante)) {
            idComercioOperacion = (comercioHeader != null) ? comercioHeader : null;
        } else {
            idComercioOperacion = idComercioContexto;
        }

        return new ContextoSolicitante(rolSolicitante, idComercioOperacion);
    }

    private record ContextoSolicitante(String rolSolicitante, Long idComercioOperacion) {}
}
