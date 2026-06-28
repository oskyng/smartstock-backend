package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.ComercioRepository;
import com.osanzana.smartstock.auth.core.repositories.RolRepository;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.ConflictException;
import com.osanzana.smartstock.auth.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.auth.shared.exception.UnauthorizedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private UsuarioRequestDTO requestDTO;
    private Rol rolOperador;
    private Comercio comercio;

    @BeforeEach
    void setUp() {
        requestDTO = UsuarioRequestDTO.builder()
                .rut("12345678-9")
                .nombre("Test")
                .apellido("User")
                .email("test@example.com")
                .password("password")
                .idRol(1L)
                .idComercio(1L)
                .build();

        rolOperador = Rol.builder()
                .id(1L)
                .nombre("OPERADOR_INVENTARIO")
                .build();

        comercio = Comercio.builder()
                .id(1L)
                .razonSocial("Comercio Test")
                .build();
    }

    @Test
    void crearUsuario_Success_AdminSistema() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolOperador));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        Usuario usuarioGuardado = Usuario.builder()
                .id(1L)
                .rut(requestDTO.getRut())
                .nombre(requestDTO.getNombre())
                .apellido(requestDTO.getApellido())
                .email(requestDTO.getEmail())
                .rol(rolOperador)
                .comercio(comercio)
                .activo(1)
                .build();
        
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponseDTO response = usuarioService.crearUsuario(requestDTO, null, "ADMIN_SISTEMA");

        assertNotNull(response);
        assertEquals(requestDTO.getEmail(), response.getEmail());
        assertEquals("OPERADOR_INVENTARIO", response.getRol());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crearUsuario_Success_GerenteTienda() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolOperador));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        Usuario usuarioGuardado = Usuario.builder()
                .id(1L)
                .rut(requestDTO.getRut())
                .rol(rolOperador)
                .comercio(comercio)
                .build();
        
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        // El ID del comercio viene del contexto para el Gerente
        UsuarioResponseDTO response = usuarioService.crearUsuario(requestDTO, 1L, "GERENTE_TIENDA");

        assertNotNull(response);
        assertEquals(1L, response.getIdComercio());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crearUsuario_Conflict_EmailExists() {
        when(usuarioRepository.findByEmail(requestDTO.getEmail())).thenReturn(Optional.of(new Usuario()));

        assertThrows(ConflictException.class, () -> 
            usuarioService.crearUsuario(requestDTO, null, "ADMIN_SISTEMA"));
    }

    @Test
    void crearUsuario_Conflict_RutExists() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(requestDTO.getRut())).thenReturn(Optional.of(new Usuario()));

        assertThrows(ConflictException.class, () -> 
            usuarioService.crearUsuario(requestDTO, null, "ADMIN_SISTEMA"));
    }

    @Test
    void crearUsuario_NotFound_RolNotExists() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            usuarioService.crearUsuario(requestDTO, null, "ADMIN_SISTEMA"));
    }

    @Test
    void crearUsuario_Unauthorized_GerenteCreatesAdmin() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        
        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ADMIN_SISTEMA");
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolAdmin));

        assertThrows(UnauthorizedActionException.class, () -> 
            usuarioService.crearUsuario(requestDTO, 1L, "GERENTE_TIENDA"));
    }

    @Test
    void crearUsuario_Unauthorized_InvalidSolicitante() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findById(1L)).thenReturn(Optional.of(rolOperador));

        assertThrows(UnauthorizedActionException.class, () -> 
            usuarioService.crearUsuario(requestDTO, 1L, "OPERADOR_INVENTARIO"));
    }
}
