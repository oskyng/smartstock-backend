package com.osanzana.smartstock.auth.autenticacion.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.auth.autenticacion.services.AuthService;
import com.osanzana.smartstock.auth.shared.dto.request.AuthRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.AuthResponseDTO;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.context.annotation.Import(AuthControllerTest.JwtTestConfig.class)
class AuthControllerTest {

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
    private AuthService authService;

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
    void login_Success() throws Exception {
        AuthRequestDTO request = AuthRequestDTO.builder()
                .email("test@example.com")
                .password("password123")
                .build();
        
        AuthResponseDTO response = AuthResponseDTO.builder()
                .token("jwt-token")
                .email("test@example.com")
                .rol("ADMIN_SISTEMA")
                .build();

        when(authService.login(any(AuthRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
