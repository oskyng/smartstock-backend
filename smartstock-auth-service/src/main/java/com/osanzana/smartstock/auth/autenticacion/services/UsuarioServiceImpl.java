package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.RolRepository;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.repositories.ComercioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.BusinessException;
import com.osanzana.smartstock.auth.shared.exception.ConflictException;
import com.osanzana.smartstock.auth.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.auth.shared.exception.UnauthorizedActionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ComercioRepository comercioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto, Long idComercioContexto, String rolSolicitante) {
        log.info("Intento de creación de usuario: email={}, rut={}, solicitante={}, contextoComercio={}", 
                dto.getEmail(), dto.getRut(), rolSolicitante, idComercioContexto);

        // 1. Verificar duplicados
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ConflictException("El email ya existe en el sistema.");
        }
        if (usuarioRepository.findByRut(dto.getRut()).isPresent()) {
            throw new ConflictException("El RUT ya existe en el sistema.");
        }

        // 2. Obtener Rol
        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new ResourceNotFoundException("Rol con ID " + dto.getIdRol() + " no encontrado."));

        // 3. Control Multi-tenant (RBAC)
        Long idComercioFinal = null;
        
        if ("GERENTE_TIENDA".equals(rolSolicitante)) {
            // Forzar comercio del gerente
            idComercioFinal = idComercioContexto;
            // Validar roles permitidos
            if (!"OPERADOR_INVENTARIO".equals(rol.getNombre()) && !"REPONEDOR_SALA".equals(rol.getNombre())) {
                throw new UnauthorizedActionException("El Gerente solo puede crear roles OPERADOR_INVENTARIO o REPONEDOR_SALA.");
            }
        } else if ("ADMIN_SISTEMA".equals(rolSolicitante)) {
            // ADMIN_SISTEMA permite cualquier rol y comercio provisto en DTO
            // REGLA: Si el rol es ADMIN_SISTEMA, el comercio DEBE ser NULL
            if ("ADMIN_SISTEMA".equals(rol.getNombre())) {
                idComercioFinal = null;
            } else {
                idComercioFinal = dto.getIdComercio();
                if (idComercioFinal == null) {
                    throw new BusinessException("Los roles locales deben tener un ID de comercio asociado.");
                }
            }
        } else {
            throw new UnauthorizedActionException("El rol solicitante no tiene permisos para crear usuarios.");
        }

        // 4. Obtener Entidad Comercio si aplica
        Comercio comercio = null;
        if (idComercioFinal != null) {
            Long finalId = idComercioFinal;
            comercio = comercioRepository.findById(finalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Comercio con ID " + finalId + " no encontrado."));
        }

        // 5. Mapear y persistir (BCrypt) con Builder
        Usuario usuario = Usuario.builder()
                .rut(dto.getRut())
                .nombre(dto.getNombre())
                .apellido(dto.getApellido())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .rol(rol)
                .comercio(comercio)
                .activo(1)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado exitosamente: ID={}", guardado.getId());

        return mapToResponseDTO(guardado);
    }

    private UsuarioResponseDTO mapToResponseDTO(Usuario usuario) {
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
