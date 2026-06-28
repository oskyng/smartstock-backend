package com.osanzana.smartstock.alert.alertas.web;

import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaControllerTest {

    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private AlertaController alertaController;

    private AlertaResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = AlertaResponseDTO.builder()
                .id(1L)
                .mensaje("Test Alerta")
                .estado("PENDIENTE")
                .rolAsignado("REPONEDOR_SALA")
                .fechaLimite(LocalDateTime.now().plusHours(24))
                .build();
    }

    @Test
    void listarPendientes_Success() {
        when(alertaService.listarPendientesPorComercio(1L)).thenReturn(Collections.singletonList(responseDTO));

        ResponseEntity<List<AlertaResponseDTO>> response = alertaController.listarPendientes(1L);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("Test Alerta", response.getBody().get(0).getMensaje());
        verify(alertaService).listarPendientesPorComercio(1L);
    }
}
