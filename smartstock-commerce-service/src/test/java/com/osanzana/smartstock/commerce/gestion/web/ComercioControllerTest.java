package com.osanzana.smartstock.commerce.gestion.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.commerce.gestion.services.ComercioService;
import com.osanzana.smartstock.commerce.shared.dto.request.ComercioRequestDTO;
import com.osanzana.smartstock.commerce.shared.dto.response.ComercioResponseDTO;
import com.osanzana.smartstock.commerce.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;
import com.osanzana.smartstock.commerce.shared.security.SecurityConfig;

@WebMvcTest(ComercioController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class ComercioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComercioService comercioService;

    @MockBean
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private ComercioResponseDTO responseDTO;
    private ComercioRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = ComercioResponseDTO.builder()
                .id(1L)
                .rutEmpresa("12.345.678-9")
                .razonSocial("Tienda Test")
                .rubro("Retail")
                .fechaRegistro(LocalDate.now())
                .estado("ACTIVO")
                .build();

        requestDTO = ComercioRequestDTO.builder()
                .rutEmpresa("12.345.678-9")
                .razonSocial("Tienda Test")
                .rubro("Retail")
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN_SISTEMA")
    void crearComercio_Success() throws Exception {
        when(comercioService.crearComercio(any(ComercioRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/comercios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.razonSocial").value("Tienda Test"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SISTEMA")
    void listarComercios_Success() throws Exception {
        when(comercioService.listarComercios()).thenReturn(Collections.singletonList(responseDTO));

        mockMvc.perform(get("/api/v1/comercios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @WithMockUser(roles = "GERENTE_TIENDA")
    void obtenerComercio_Success() throws Exception {
        when(comercioService.obtenerPorId(anyLong(), anyString(), any())).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/comercios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "GERENTE_TIENDA")
    void actualizarComercio_Success() throws Exception {
        when(comercioService.updateComercio(anyLong(), any(ComercioRequestDTO.class), anyString(), any())).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/comercios/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN_SISTEMA")
    void eliminarComercio_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/comercios/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "GERENTE_TIENDA")
    void listarComercios_GerenteRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/comercios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GERENTE_TIENDA")
    void crearComercio_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/comercios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }
}
