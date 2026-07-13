package com.osanzana.smartstock.inventory.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.inventory.inventario.services.CategoriaService;
import com.osanzana.smartstock.inventory.shared.dto.request.CategoriaRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.CategoriaResponseDTO;
import com.osanzana.smartstock.inventory.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoriaService categoriaService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarCategorias_Success() throws Exception {
        CategoriaResponseDTO response = CategoriaResponseDTO.builder().id(1L).nombre("Bebidas").build();
        when(categoriaService.listarPorComercio(anyLong())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/categorias")
                .header("X-Comercio-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Bebidas"));
    }

    @Test
    void crearCategoria_Success() throws Exception {
        CategoriaRequestDTO request = CategoriaRequestDTO.builder().nombre("Bebidas").build();
        CategoriaResponseDTO response = CategoriaResponseDTO.builder().id(1L).nombre("Bebidas").build();

        when(categoriaService.crear(any(CategoriaRequestDTO.class), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/v1/categorias")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Bebidas"));
    }

    @Test
    void crearCategoria_ValidationFailed() throws Exception {
        CategoriaRequestDTO request = CategoriaRequestDTO.builder().nombre("").build();

        mockMvc.perform(post("/api/v1/categorias")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarCategorias_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isBadRequest());
    }
}
