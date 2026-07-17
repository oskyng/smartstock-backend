package com.osanzana.smartstock.auth.autenticacion.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.auth.autenticacion.services.UsuarioService;
import com.osanzana.smartstock.auth.core.entities.Comercio;
import com.osanzana.smartstock.auth.core.entities.Rol;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.CambiarContrasenaRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioCreateRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.UsuarioUpdateRequestDTO;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
                .principal(new UsernamePasswordAuthenticationToken("admin@test.cl", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "gerente@test.cl", roles = "GERENTE_TIENDA")
    void listarUsuarios_Success() throws Exception {
        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        Usuario solicitante = Usuario.builder().id(1L).email("gerente@test.cl").rol(rolGerente).comercio(comercio).build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));
        when(usuarioService.listar(eq(1L), eq("GERENTE_TIENDA")))
                .thenReturn(List.of(UsuarioResponseDTO.builder().id(5L).email("op@test.cl").build()));

        mockMvc.perform(get("/api/v1/usuarios")
                .header("X-Comercio-ID", "1")
                .principal(new UsernamePasswordAuthenticationToken("gerente@test.cl", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("op@test.cl"));
    }

    @Test
    @WithMockUser(username = "gerente@test.cl", roles = "GERENTE_TIENDA")
    void actualizarUsuario_Success() throws Exception {
        UsuarioUpdateRequestDTO request = UsuarioUpdateRequestDTO.builder()
                .nombre("Nuevo").apellido("Nombre").email("op@test.cl").idRol(3L).build();

        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        Usuario solicitante = Usuario.builder().id(1L).email("gerente@test.cl").rol(rolGerente).comercio(comercio).build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));
        when(usuarioService.actualizar(eq(5L), any(UsuarioUpdateRequestDTO.class), eq(1L), eq("GERENTE_TIENDA")))
                .thenReturn(UsuarioResponseDTO.builder().id(5L).nombre("Nuevo").build());

        mockMvc.perform(put("/api/v1/usuarios/5")
                .header("X-Comercio-ID", "1")
                .principal(new UsernamePasswordAuthenticationToken("gerente@test.cl", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
    }

    @Test
    @WithMockUser(username = "gerente@test.cl", roles = "GERENTE_TIENDA")
    void eliminarUsuario_Success() throws Exception {
        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        Usuario solicitante = Usuario.builder().id(1L).email("gerente@test.cl").rol(rolGerente).comercio(comercio).build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));

        mockMvc.perform(delete("/api/v1/usuarios/5")
                .header("X-Comercio-ID", "1")
                .principal(new UsernamePasswordAuthenticationToken("gerente@test.cl", null)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "gerente@test.cl", roles = "GERENTE_TIENDA")
    void reactivarUsuario_Success() throws Exception {
        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        Usuario solicitante = Usuario.builder().id(1L).email("gerente@test.cl").rol(rolGerente).comercio(comercio).build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));
        when(usuarioService.reactivar(eq(5L), eq(1L), eq("GERENTE_TIENDA")))
                .thenReturn(UsuarioResponseDTO.builder().id(5L).activo(1).build());

        mockMvc.perform(patch("/api/v1/usuarios/5/reactivar")
                .header("X-Comercio-ID", "1")
                .principal(new UsernamePasswordAuthenticationToken("gerente@test.cl", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(1));
    }

    @Test
    @WithMockUser(username = "gerente@test.cl", roles = "GERENTE_TIENDA")
    void cambiarContrasena_Success() throws Exception {
        CambiarContrasenaRequestDTO request = new CambiarContrasenaRequestDTO();
        request.setNuevaContrasena("claveNueva123");

        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        Usuario solicitante = Usuario.builder().id(1L).email("gerente@test.cl").rol(rolGerente).comercio(comercio).build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));

        mockMvc.perform(patch("/api/v1/usuarios/5/password")
                .header("X-Comercio-ID", "1")
                .principal(new UsernamePasswordAuthenticationToken("gerente@test.cl", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "gerente@test.cl", roles = "GERENTE_TIENDA")
    void cambiarContrasena_ContrasenaMuyCorta_Retorna400() throws Exception {
        CambiarContrasenaRequestDTO request = new CambiarContrasenaRequestDTO();
        request.setNuevaContrasena("corta");

        Rol rolGerente = Rol.builder().id(2L).nombre("GERENTE_TIENDA").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        Usuario solicitante = Usuario.builder().id(1L).email("gerente@test.cl").rol(rolGerente).comercio(comercio).build();

        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(solicitante));

        mockMvc.perform(patch("/api/v1/usuarios/5/password")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
