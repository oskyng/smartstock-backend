package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.CodigoRecuperacion;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.CodigoRecuperacionRepository;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.exception.BusinessException;
import com.osanzana.smartstock.auth.shared.exception.TooManyAttemptsException;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import com.osanzana.smartstock.auth.shared.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CodigoRecuperacionRepository codigoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private EmailService emailService;
    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private RecuperacionService recuperacionService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Diego")
                .apellido("Silva")
                .email("diego@test.com")
                .passwordHash("hash-viejo")
                .build();
    }

    @Test
    void solicitarRecuperacion_UsuarioExiste_InvalidaCodigosPreviosYEnviaCorreo() {
        CodigoRecuperacion codigoPrevio = CodigoRecuperacion.builder()
                .id(9L)
                .usuario(usuario)
                .codigoVerificador("111111")
                .utilizado("N")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(3))
                .build();

        when(usuarioRepository.findByEmail("diego@test.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findByUsuarioIdAndUtilizado(1L, "N")).thenReturn(java.util.List.of(codigoPrevio));

        recuperacionService.solicitarRecuperacion("diego@test.com");

        // El código previo se invalida (fila por fila, no vía @Modifying bulk UPDATE: ver
        // comentario en CodigoRecuperacionRepository sobre ORA-12838 en Oracle ADB).
        assertEquals("S", codigoPrevio.getUtilizado());

        ArgumentCaptor<CodigoRecuperacion> captor = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRepository, times(2)).save(captor.capture());
        CodigoRecuperacion nuevo = captor.getAllValues().get(1);
        assertEquals(usuario, nuevo.getUsuario());
        assertEquals("N", nuevo.getUtilizado());
        assertEquals(6, nuevo.getCodigoVerificador().length());
        assertTrue(nuevo.getCodigoVerificador().matches("\\d{6}"));
        assertTrue(nuevo.getFechaExpiracion().isAfter(LocalDateTime.now()));

        verify(emailService).enviarCodigoRecuperacion(eq("diego@test.com"), eq("Diego"), anyString());
    }

    @Test
    void solicitarRecuperacion_UsuarioNoExiste_NoHaceNadaNiFalla() {
        when(usuarioRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> recuperacionService.solicitarRecuperacion("fantasma@test.com"));

        verify(codigoRepository, never()).findByUsuarioIdAndUtilizado(anyLong(), anyString());
        verify(codigoRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void verificarCodigo_CodigoValido_LoInvalidaYRetornaToken() {
        CodigoRecuperacion codigo = CodigoRecuperacion.builder()
                .id(10L)
                .usuario(usuario)
                .codigoVerificador("123456")
                .utilizado("N")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        when(usuarioRepository.findByEmail("diego@test.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findByUsuarioIdAndCodigoVerificadorAndUtilizado(1L, "123456", "N"))
                .thenReturn(Optional.of(codigo));
        when(jwtUtils.generarTokenRestablecimiento("diego@test.com")).thenReturn("reset-token");

        String token = recuperacionService.verificarCodigo("diego@test.com", "123456");

        assertEquals("reset-token", token);
        assertEquals("S", codigo.getUtilizado());
        verify(codigoRepository).save(codigo);
        verify(rateLimiterService).verificarNoBloqueado("diego@test.com");
        verify(rateLimiterService).registrarExito("diego@test.com");
    }

    @Test
    void verificarCodigo_Bloqueado_LanzaExcepcionSinConsultarNada() {
        doThrow(new TooManyAttemptsException("Demasiados intentos fallidos"))
                .when(rateLimiterService).verificarNoBloqueado("diego@test.com");

        assertThrows(TooManyAttemptsException.class,
                () -> recuperacionService.verificarCodigo("diego@test.com", "123456"));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void verificarCodigo_CodigoIncorrecto_RegistraFallo() {
        when(usuarioRepository.findByEmail("diego@test.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findByUsuarioIdAndCodigoVerificadorAndUtilizado(1L, "000000", "N"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> recuperacionService.verificarCodigo("diego@test.com", "000000"));
        verify(rateLimiterService).registrarFallo("diego@test.com");
        verify(rateLimiterService, never()).registrarExito(anyString());
    }

    @Test
    void verificarCodigo_CodigoExpirado_LanzaBusinessException() {
        CodigoRecuperacion codigo = CodigoRecuperacion.builder()
                .id(10L)
                .usuario(usuario)
                .codigoVerificador("123456")
                .utilizado("N")
                .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                .build();

        when(usuarioRepository.findByEmail("diego@test.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findByUsuarioIdAndCodigoVerificadorAndUtilizado(1L, "123456", "N"))
                .thenReturn(Optional.of(codigo));

        assertThrows(BusinessException.class, () -> recuperacionService.verificarCodigo("diego@test.com", "123456"));
        verify(codigoRepository, never()).save(any());
    }

    @Test
    void verificarCodigo_CodigoInexistente_LanzaBusinessException() {
        when(usuarioRepository.findByEmail("diego@test.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findByUsuarioIdAndCodigoVerificadorAndUtilizado(1L, "000000", "N"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> recuperacionService.verificarCodigo("diego@test.com", "000000"));
    }

    @Test
    void verificarCodigo_UsuarioInexistente_LanzaBusinessException() {
        when(usuarioRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> recuperacionService.verificarCodigo("fantasma@test.com", "123456"));
        verifyNoInteractions(codigoRepository);
    }

    @Test
    void restablecerContrasena_ActualizaHashConBCrypt() {
        when(usuarioRepository.findByEmail("diego@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("nuevaClave123")).thenReturn("hash-nuevo");

        recuperacionService.restablecerContrasena("diego@test.com", "nuevaClave123");

        assertEquals("hash-nuevo", usuario.getPasswordHash());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void restablecerContrasena_UsuarioInexistente_LanzaBusinessException() {
        when(usuarioRepository.findByEmail("fantasma@test.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> recuperacionService.restablecerContrasena("fantasma@test.com", "nuevaClave123"));
    }
}
