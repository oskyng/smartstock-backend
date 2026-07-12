package com.osanzana.smartstock.auth.autenticacion.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.auth.autenticacion.services.UsuarioService;
import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioCreateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.UsuarioResponseDTO;
import com.osanzana.smartstock.auth.shared.security.JwtAuthenticationFilter;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import(UsuarioControllerTest.JwtTestConfig.class)
class UsuarioControllerTest {

    @TestConfiguration
    static class JwtTestConfig {
        @Bean
        public JwtUtils jwtUtils() {
            return org.mockito.Mockito.mock(JwtUtils.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @MockBean
    private org.springframework.security.authentication.AuthenticationProvider authenticationProvider;

    @MockBean
    private com.osanzana.smartstock.auth.shared.security.CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @MockBean
    private com.osanzana.smartstock.auth.shared.security.CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearAdminSistema_Success() throws Exception {
        UsuarioCreateRequestDTO request = UsuarioCreateRequestDTO.builder()
                .rut("12345678-9")
                .nombre("Admin")
                .apellido("Sistema")
                .email("admin@test.cl")
                .password("pass123")
                .build();

        UsuarioResponseDTO response = UsuarioResponseDTO.builder()
                .email("admin@test.cl")
                .rol("ADMIN_SISTEMA")
                .build();

        when(usuarioService.crearAdminSistema(any(UsuarioCreateRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/usuarios/admin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin@test.cl"));
    }

    @Test
    @WithMockUser(username = "admin@test.cl", roles = "ADMIN_SISTEMA")
    void crearUsuario_Success_Admin() throws Exception {
        UsuarioRequestDTO request = new UsuarioRequestDTO();
        request.setRut("98765432-1");
        request.setNombre("Juan");
        request.setApellido("Perez");
        request.setEmail("repo@test.cl");
        request.setPassword("password123");
        request.setIdRol(3L);
        request.setIdComercio(1L);

        Rol rolAdmin = Rol.builder().id(1L).nombre("ADMIN_SISTEMA").build();
        Usuario solicitante = Usuario.builder()
                .id(1L)
                .email("admin@test.cl")
                .rol(rolAdmin)
                .build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));
        
        UsuarioResponseDTO responseDTO = UsuarioResponseDTO.builder()
                .email("repo@test.cl")
                .idComercio(1L)
                .build();
        
        when(usuarioService.crearUsuario(any(), any(), any()))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/usuarios")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isCreated());
    }
}
