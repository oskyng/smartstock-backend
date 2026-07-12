package com.osanzana.smartstock.inventory.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.inventory.inventario.services.ProductoService;
import com.osanzana.smartstock.inventory.shared.dto.request.ProductoRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProductoResponseDTO;
import com.osanzana.smartstock.inventory.shared.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductoService productoService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarProductos_Success() throws Exception {
        ProductoResponseDTO response = ProductoResponseDTO.builder().id(1L).nombre("Test").build();
        when(productoService.listarPorComercio(anyLong())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/productos")
                .header("X-Comercio-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Test"));
    }

    @Test
    void guardarProducto_Success() throws Exception {
        ProductoRequestDTO request = ProductoRequestDTO.builder()
                .nombre("Nuevo")
                .codigoBarra("SKU-123")
                .precioBase(new BigDecimal("500"))
                .idCategoria(1L)
                .build();
        ProductoResponseDTO response = ProductoResponseDTO.builder().id(2L).nombre("Nuevo").build();
        
        when(productoService.guardar(any(ProductoRequestDTO.class), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/v1/productos")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Nuevo"));
    }
    @Test
    void obtenerProducto_Success() throws Exception {
        ProductoResponseDTO response = ProductoResponseDTO.builder().id(1L).nombre("Test").build();
        when(productoService.obtenerPorId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Test"));
    }

    @Test
    void guardarProducto_ValidationFailed() throws Exception {
        ProductoRequestDTO request = ProductoRequestDTO.builder().nombre("").build(); // Nombre vacío

        mockMvc.perform(post("/api/v1/productos")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarProductos_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isBadRequest());
    }
}
