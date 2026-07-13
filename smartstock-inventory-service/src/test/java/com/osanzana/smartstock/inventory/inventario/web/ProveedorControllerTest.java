package com.osanzana.smartstock.inventory.inventario.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osanzana.smartstock.inventory.inventario.services.ProveedorService;
import com.osanzana.smartstock.inventory.shared.dto.request.ProveedorRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProveedorResponseDTO;
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

@WebMvcTest(ProveedorController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProveedorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProveedorService proveedorService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarProveedores_Success() throws Exception {
        ProveedorResponseDTO response = ProveedorResponseDTO.builder()
                .id(1L).rutEmpresa("76.111.111-1").razonSocial("Proveedor Uno").contactoEmail("contacto@proveedor.cl").build();
        when(proveedorService.listarPorComercio(anyLong())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/proveedores")
                .header("X-Comercio-ID", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razonSocial").value("Proveedor Uno"));
    }

    @Test
    void crearProveedor_Success() throws Exception {
        ProveedorRequestDTO request = ProveedorRequestDTO.builder()
                .rutEmpresa("76.111.111-1").razonSocial("Proveedor Uno").contactoEmail("contacto@proveedor.cl").build();
        ProveedorResponseDTO response = ProveedorResponseDTO.builder()
                .id(1L).rutEmpresa("76.111.111-1").razonSocial("Proveedor Uno").contactoEmail("contacto@proveedor.cl").build();

        when(proveedorService.crear(any(ProveedorRequestDTO.class), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/v1/proveedores")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.razonSocial").value("Proveedor Uno"));
    }

    @Test
    void crearProveedor_ValidationFailed() throws Exception {
        ProveedorRequestDTO request = ProveedorRequestDTO.builder().rutEmpresa("").razonSocial("").contactoEmail("no-es-email").build();

        mockMvc.perform(post("/api/v1/proveedores")
                .header("X-Comercio-ID", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listarProveedores_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/proveedores"))
                .andExpect(status().isBadRequest());
    }
}
