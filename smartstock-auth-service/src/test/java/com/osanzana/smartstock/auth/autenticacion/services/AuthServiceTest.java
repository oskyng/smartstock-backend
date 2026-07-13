package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.AuthRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.AuthResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.BusinessException;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private AuthRequestDTO authRequest;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        authRequest = AuthRequestDTO.builder()
                .email("test@example.com")
                .password("password")
                .build();
        usuario = Usuario.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .rol(Rol.builder().nombre("ADMIN").build())
                .build();
    }

    @Test
    void login_Success() {
        when(usuarioRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(usuario));
        when(jwtUtils.generateToken(any(UserDetails.class), anyMap())).thenReturn("mockToken");

        AuthResponseDTO response = authService.login(authRequest);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("ADMIN", response.getRol());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_UserNotFound() {
        when(usuarioRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(authRequest));
    }

    @Test
    void login_Success_WithComercio() {
        Rol rol = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(5L).razonSocial("Tienda Test").build();
        Usuario usuarioComercio = Usuario.builder()
                .email("test2@example.com")
                .passwordHash("hashed")
                .rol(rol)
                .comercio(comercio)
                .build();
        AuthRequestDTO req = AuthRequestDTO.builder().email("test2@example.com").password("pass").build();

        when(usuarioRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(usuarioComercio));
        when(jwtUtils.generateToken(any(), anyMap())).thenReturn("token2");

        AuthResponseDTO response = authService.login(req);

        assertNotNull(response);
        assertEquals(5L, response.getIdComercio());
    }
}
