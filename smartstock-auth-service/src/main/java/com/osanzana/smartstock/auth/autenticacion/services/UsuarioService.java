package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.ComercioRepository;
import com.osanzana.smartstock.auth.core.repositories.RolRepository;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioCreateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.ConflictException;
import com.osanzana.smartstock.auth.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.auth.shared.exception.UnauthorizedActionException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ComercioRepository comercioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponseDTO crearAdminSistema(UsuarioCreateRequestDTO request) {
        validarUnicidad(request.getEmail(), request.getRut());

        Rol rol = rolRepository.findByNombre("ADMIN_SISTEMA")
                .orElseThrow(() -> new ResourceNotFoundException("Rol ADMIN_SISTEMA no encontrado"));

        Usuario usuario = buildUsuario(request, rol, null);
        Usuario guardado = usuarioRepository.save(usuario);
        return mapToResponse(guardado);
    }

    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO request, Long idComercioContexto, String rolSolicitante) {
        validarUnicidad(request.getEmail(), request.getRut());

        Rol rolDestino = rolRepository.findById(request.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        validarJerarquia(rolSolicitante, rolDestino.getNombre());

        Long idComercioOperacion = determinarComercio(request, idComercioContexto, rolSolicitante);
        Comercio comercio = null;
        if (idComercioOperacion != null) {
            comercio = comercioRepository.findById(idComercioOperacion)
                    .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));
        }

        Usuario usuario = Usuario.builder()
                .rut(request.getRut())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(rolDestino)
                .comercio(comercio)
                .activo(1)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        return mapToResponse(guardado);
    }

    private void validarUnicidad(String email, String rut) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("El email ya está registrado");
        }
        if (usuarioRepository.findByRut(rut).isPresent()) {
            throw new ConflictException("El RUT ya está registrado");
        }
    }

    private void validarJerarquia(String rolSolicitante, String rolDestino) {
        if ("ADMIN_SISTEMA".equals(rolSolicitante)) {
            return;
        }

        if ("GERENTE_TIENDA".equals(rolSolicitante)) {
            if ("ADMIN_SISTEMA".equals(rolDestino) || "GERENTE_TIENDA".equals(rolDestino)) {
                throw new UnauthorizedActionException("Un GERENTE_TIENDA no puede crear administradores o gerentes.");
            }
            return;
        }

        throw new UnauthorizedActionException("No tienes permisos para crear usuarios.");
    }

    private Long determinarComercio(UsuarioRequestDTO request, Long idComercioContexto, String rolSolicitante) {
        if ("ADMIN_SISTEMA".equals(rolSolicitante)) {
            return request.getIdComercio();
        }
        return idComercioContexto;
    }

    private Usuario buildUsuario(UsuarioCreateRequestDTO request, Rol rol, Comercio comercio) {
        return Usuario.builder()
                .rut(request.getRut())
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(rol)
                .comercio(comercio)
                .activo(1)
                .build();
    }

    private UsuarioResponseDTO mapToResponse(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .rut(usuario.getRut())
                .nombre(usuario.getNombre())
                .apellido(usuario.getApellido())
                .email(usuario.getEmail())
                .rol(usuario.getRol().getNombre())
                .idComercio(usuario.getComercio() != null ? usuario.getComercio().getId() : null)
                .activo(usuario.getActivo())
                .build();
    }
}
