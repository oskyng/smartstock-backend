package com.osanzana.smartstock.finance.finanzas.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.finance.finanzas.services.ReglaDepreciacionService;
import com.osanzana.smartstock.finance.shared.dto.request.ReglaDepreciacionRequestDTO;
import com.osanzana.smartstock.finance.shared.dto.response.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.finance.shared.security.CustomAccessDeniedHandler;
import com.osanzana.smartstock.finance.shared.security.CustomAuthenticationEntryPoint;
import com.osanzana.smartstock.finance.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReglaDepreciacionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class ReglaDepreciacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReglaDepreciacionService service;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarReglas_Success() throws Exception {
        ReglaDepreciacionResponseDTO response = ReglaDepreciacionResponseDTO.builder().id(1L).nombreCategoria("Test").build();
        when(service.listarPorComercio(anyLong())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reglas-depreciacion")
                .header("X-Comercio-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreCategoria").value("Test"));
    }

    @Test
    void crearRegla_Success() throws Exception {
        ReglaDepreciacionRequestDTO request = ReglaDepreciacionRequestDTO.builder()
                .idCategoria(1L)
                .diasCriticosMin(5)
                .porcentajeDescuento(new BigDecimal("10"))
                .build();
        ReglaDepreciacionResponseDTO response = ReglaDepreciacionResponseDTO.builder().id(2L).nombreCategoria("Test").build();

        when(service.guardar(any(ReglaDepreciacionRequestDTO.class), anyLong(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/reglas-depreciacion")
                .header("X-Comercio-ID", "1")
                .principal(() -> "gerente@test.cl")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreCategoria").value("Test"));
    }
}
