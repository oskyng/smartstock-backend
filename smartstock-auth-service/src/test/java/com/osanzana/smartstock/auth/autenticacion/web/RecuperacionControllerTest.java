package com.osanzana.smartstock.auth.autenticacion.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.auth.autenticacion.services.RecuperacionService;
import com.osanzana.smartstock.auth.shared.dto.request.ForgotPasswordRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.ResetPasswordRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.VerifyCodeRequestDTO;
import com.osanzana.smartstock.auth.shared.security.JwtAuthenticationFilter;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecuperacionController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecuperacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecuperacionService recuperacionService;

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
    void forgotPassword_SiempreRetornaMensajeGenerico() throws Exception {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setCorreo("diego@test.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").exists());

        verify(recuperacionService).solicitarRecuperacion("diego@test.com");
    }

    @Test
    void forgotPassword_CorreoInvalido_Retorna400() throws Exception {
        ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO();
        request.setCorreo("no-es-un-correo");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recuperacionService);
    }

    @Test
    void verifyCode_CodigoValido_RetornaToken() throws Exception {
        VerifyCodeRequestDTO request = new VerifyCodeRequestDTO();
        request.setCorreo("diego@test.com");
        request.setCodigo("123456");

        when(recuperacionService.verificarCodigo("diego@test.com", "123456")).thenReturn("reset-token");

        mockMvc.perform(post("/api/v1/auth/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("reset-token"));
    }

    @Test
    void verifyCode_CodigoConFormatoInvalido_Retorna400() throws Exception {
        VerifyCodeRequestDTO request = new VerifyCodeRequestDTO();
        request.setCorreo("diego@test.com");
        request.setCodigo("12");

        mockMvc.perform(post("/api/v1/auth/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(recuperacionService);
    }

    @Test
    void resetPassword_TokenDeRestablecimientoValido_ActualizaContrasena() throws Exception {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setNuevaContrasena("nuevaClave123");

        when(jwtUtils.extractPurpose("reset-token")).thenReturn("PASSWORD_RESET");
        when(jwtUtils.extractUsername("reset-token")).thenReturn("diego@test.com");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("Authorization", "Bearer reset-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(recuperacionService).restablecerContrasena("diego@test.com", "nuevaClave123");
    }

    @Test
    void resetPassword_TokenDeLoginNormal_EsRechazado() throws Exception {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setNuevaContrasena("nuevaClave123");

        when(jwtUtils.extractPurpose("login-token")).thenReturn(null);

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("Authorization", "Bearer login-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(recuperacionService, never()).restablecerContrasena(anyString(), anyString());
    }

    @Test
    void resetPassword_SinHeaderAuthorization_Retorna400() throws Exception {
        ResetPasswordRequestDTO request = new ResetPasswordRequestDTO();
        request.setNuevaContrasena("nuevaClave123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
