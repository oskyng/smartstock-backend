package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.ComercioRepository;
import com.osanzana.smartstock.auth.core.repositories.RolRepository;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioCreateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioUpdateRequestDTO;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RolRepository rolRepository;
    @Mock
    private ComercioRepository comercioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioCreateRequestDTO requestDTO;
    private Rol rolAdmin;

    @BeforeEach
    void setUp() {
        requestDTO = UsuarioCreateRequestDTO.builder()
                .rut("12345678-9")
                .nombre("Test")
                .apellido("User")
                .email("test@example.com")
                .password("password")
                .build();

        rolAdmin = Rol.builder()
                .id(1L)
                .nombre("ADMIN_SISTEMA")
                .build();
    }

    @Test
    void crearAdminSistema_Success() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findByNombre("ADMIN_SISTEMA")).thenReturn(Optional.of(rolAdmin));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        
        Usuario usuarioGuardado = Usuario.builder()
                .id(1L)
                .rut(requestDTO.getRut())
                .nombre(requestDTO.getNombre())
                .apellido(requestDTO.getApellido())
                .email(requestDTO.getEmail())
                .rol(rolAdmin)
                .activo(1)
                .build();
        
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        UsuarioResponseDTO response = usuarioService.crearAdminSistema(requestDTO);

        assertNotNull(response);
        assertEquals(requestDTO.getEmail(), response.getEmail());
        assertEquals("ADMIN_SISTEMA", response.getRol());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crearAdminSistema_Conflict_EmailExists() {
        when(usuarioRepository.findByEmail(requestDTO.getEmail())).thenReturn(Optional.of(new Usuario()));

        assertThrows(ConflictException.class, () -> 
            usuarioService.crearAdminSistema(requestDTO));
    }

    @Test
    void crearAdminSistema_Conflict_RutExists() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(requestDTO.getRut())).thenReturn(Optional.of(new Usuario()));

        assertThrows(ConflictException.class, () -> 
            usuarioService.crearAdminSistema(requestDTO));
    }

    @Test
    void crearAdminSistema_NotFound_RolNotExists() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(anyString())).thenReturn(Optional.empty());
        when(rolRepository.findByNombre("ADMIN_SISTEMA")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            usuarioService.crearAdminSistema(requestDTO));
    }

    @Test
    void crearUsuario_Success_AdminSolicitante() {
        UsuarioRequestDTO userReq = UsuarioRequestDTO.builder()
                .rut("11222333-4")
                .nombre("Gerente")
                .apellido("Tienda")
                .email("gerente@tienda.cl")
                .password("pass123")
                .idRol(2L)
                .idComercio(1L)
                .build();

        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).razonSocial("Tienda 1").build();

        when(usuarioRepository.findByEmail(userReq.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(userReq.getRut())).thenReturn(Optional.empty());
        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolGerente));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        UsuarioResponseDTO response = usuarioService.crearUsuario(userReq, null, "ADMIN_SISTEMA");

        assertNotNull(response);
        assertEquals(userReq.getEmail(), response.getEmail());
        assertEquals(1L, response.getIdComercio());
    }

    @Test
    void crearUsuario_Success_GerenteSolicitante() {
        UsuarioRequestDTO userReq = UsuarioRequestDTO.builder()
                .rut("44555666-7")
                .nombre("Reponedor")
                .apellido("Sala")
                .email("repo@tienda.cl")
                .password("pass123")
                .idRol(3L)
                .idComercio(2L) // Intenta forzar otro comercio
                .build();

        Rol rolRepo = Rol.builder().id(3L).nombre("REPONEDOR_SALA").build();
        Comercio comercioPropio = Comercio.builder().id(1L).razonSocial("Tienda Propia").build();

        when(usuarioRepository.findByEmail(userReq.getEmail())).thenReturn(Optional.empty());
        when(usuarioRepository.findByRut(userReq.getRut())).thenReturn(Optional.empty());
        when(rolRepository.findById(3L)).thenReturn(Optional.of(rolRepo));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercioPropio));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        // idComercioContexto = 1L. Debe ignorar el 2L del request.
        UsuarioResponseDTO response = usuarioService.crearUsuario(userReq, 1L, "GERENTE_TIENDA");

        assertNotNull(response);
        assertEquals(1L, response.getIdComercio());
    }

    @Test
    void crearUsuario_Unauthorized_GerenteCreatesGerente() {
        UsuarioRequestDTO userReq = UsuarioRequestDTO.builder()
                .idRol(2L)
                .build();
        Rol rolGerente = Rol.builder().nombre("GERENTE_TIENDA").build();

        when(rolRepository.findById(2L)).thenReturn(Optional.of(rolGerente));

        assertThrows(UnauthorizedActionException.class, () -> 
            usuarioService.crearUsuario(userReq, 1L, "GERENTE_TIENDA"));
    }

    @Test
    void crearUsuario_Unauthorized_LowRole() {
        UsuarioRequestDTO userReq = UsuarioRequestDTO.builder()
                .idRol(3L)
                .build();
        Rol rolRepo = Rol.builder().nombre("REPONEDOR_SALA").build();

        when(rolRepository.findById(3L)).thenReturn(Optional.of(rolRepo));

        assertThrows(UnauthorizedActionException.class, () ->
            usuarioService.crearUsuario(userReq, 1L, "REPONEDOR_SALA"));
    }

    @Test
    void listar_AdminSinComercio_ListaTodos() {
        Usuario usuario = Usuario.builder().id(1L).rol(Rol.builder().nombre("OPERADOR_INVENTARIO").build()).build();
        when(usuarioRepository.findAll()).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> result = usuarioService.listar(null, "ADMIN_SISTEMA");

        assertEquals(1, result.size());
        verify(usuarioRepository).findAll();
        verify(usuarioRepository, never()).findByComercioId(anyLong());
    }

    @Test
    void listar_GerenteTienda_ListaPorComercio() {
        Usuario usuario = Usuario.builder().id(1L).rol(Rol.builder().nombre("OPERADOR_INVENTARIO").build()).build();
        when(usuarioRepository.findByComercioId(1L)).thenReturn(List.of(usuario));

        List<UsuarioResponseDTO> result = usuarioService.listar(1L, "GERENTE_TIENDA");

        assertEquals(1, result.size());
        verify(usuarioRepository).findByComercioId(1L);
    }

    @Test
    void actualizar_GerenteTienda_Success() {
        Comercio comercio = Comercio.builder().id(1L).build();
        Rol rolOperador = Rol.builder().id(3L).nombre("OPERADOR_INVENTARIO").build();
        Usuario existente = Usuario.builder().id(5L).email("op@tienda.cl").rol(rolOperador).comercio(comercio).build();
        UsuarioUpdateRequestDTO updateReq = UsuarioUpdateRequestDTO.builder()
                .nombre("Nuevo").apellido("Nombre").email("op@tienda.cl").idRol(3L).build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(rolRepository.findById(3L)).thenReturn(Optional.of(rolOperador));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        UsuarioResponseDTO result = usuarioService.actualizar(5L, updateReq, 1L, "GERENTE_TIENDA");

        assertEquals("Nuevo", result.getNombre());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void actualizar_GerenteTienda_OtroComercio_Unauthorized() {
        Comercio otroComercio = Comercio.builder().id(2L).build();
        Rol rolOperador = Rol.builder().id(3L).nombre("OPERADOR_INVENTARIO").build();
        Usuario existente = Usuario.builder().id(5L).rol(rolOperador).comercio(otroComercio).build();
        UsuarioUpdateRequestDTO updateReq = UsuarioUpdateRequestDTO.builder()
                .nombre("Nuevo").apellido("Nombre").email("op@tienda.cl").idRol(3L).build();

        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));

        assertThrows(UnauthorizedActionException.class, () ->
                usuarioService.actualizar(5L, updateReq, 1L, "GERENTE_TIENDA"));
    }

    @Test
    void actualizar_GerenteTienda_UsuarioNoGestionable_Unauthorized() {
        Comercio comercio = Comercio.builder().id(1L).build();
        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Usuario existente = Usuario.builder().id(6L).rol(rolGerente).comercio(comercio).build();
        UsuarioUpdateRequestDTO updateReq = UsuarioUpdateRequestDTO.builder()
                .nombre("Nuevo").apellido("Nombre").email("otro@tienda.cl").idRol(2L).build();

        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(existente));

        assertThrows(UnauthorizedActionException.class, () ->
                usuarioService.actualizar(6L, updateReq, 1L, "GERENTE_TIENDA"));
    }

    @Test
    void eliminar_GerenteTienda_Success() {
        Comercio comercio = Comercio.builder().id(1L).build();
        Rol rolReponedor = Rol.builder().id(4L).nombre("REPONEDOR_SALA").build();
        Usuario existente = Usuario.builder().id(7L).rol(rolReponedor).comercio(comercio).activo(1).build();

        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        usuarioService.eliminar(7L, 1L, "GERENTE_TIENDA");

        assertEquals(0, existente.getActivo());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void eliminar_AdminSistema_CualquierComercio_Success() {
        Comercio comercio = Comercio.builder().id(9L).build();
        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Usuario existente = Usuario.builder().id(8L).rol(rolGerente).comercio(comercio).activo(1).build();

        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArguments()[0]);

        usuarioService.eliminar(8L, null, "ADMIN_SISTEMA");

        assertEquals(0, existente.getActivo());
    }
}
