package com.osanzana.smartstock.inventory.inventario.web;

import com.osanzana.smartstock.inventory.inventario.services.LoteService;
import com.osanzana.smartstock.inventory.shared.dto.request.LoteRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.LoteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteControllerTest {

    @Mock
    private LoteService loteService;

    @InjectMocks
    private LoteController loteController;

    private LoteResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = LoteResponseDTO.builder()
                .id(1L)
                .nombreProducto("Producto Test")
                .cantidadActual(10)
                .build();
    }

    @Test
    void crearLote_Success() {
        LoteRequestDTO requestDTO = LoteRequestDTO.builder().idProducto(1L).build();
        when(loteService.guardarLote(any(), eq(1L))).thenReturn(responseDTO);

        ResponseEntity<LoteResponseDTO> response = loteController.crearLote(1L, requestDTO);

        assertNotNull(response);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("Producto Test", response.getBody().getNombreProducto());
    }

    @Test
    void listarLotes_Success() {
        when(loteService.listarPorComercio(1L)).thenReturn(Collections.singletonList(responseDTO));

        ResponseEntity<List<LoteResponseDTO>> response = loteController.listarLotes(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
    }
}
