package com.osanzana.smartstock.finance.finanzas.stream;

import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import com.osanzana.smartstock.finance.finanzas.services.MotorPreciosService;
import com.osanzana.smartstock.finance.shared.dto.EventoBaseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteEventConsumerTest {

    @Mock
    private MotorPreciosService motorPreciosService;

    @InjectMocks
    private LoteEventConsumer loteEventConsumer;

    @Test
    void consumeLoteCreado_Success() {
        LoteInventario lote = LoteInventario.builder().id(1L).build();
        EventoBaseDTO<LoteInventario> evento = new EventoBaseDTO<>();
        evento.setIdEvento(UUID.randomUUID().toString());
        evento.setPayload(lote);

        when(motorPreciosService.calcularPrecioDinamico(lote)).thenReturn(true);

        loteEventConsumer.consumeLoteCreado(evento);

        verify(motorPreciosService).calcularPrecioDinamico(lote);
    }

    @Test
    void consumeLoteCreado_PayloadNull() {
        EventoBaseDTO<LoteInventario> evento = new EventoBaseDTO<>();
        evento.setPayload(null);

        loteEventConsumer.consumeLoteCreado(evento);

        verify(motorPreciosService, never()).calcularPrecioDinamico(any());
    }

    @Test
    void consumeLoteCreado_ExceptionHandled() {
        LoteInventario lote = LoteInventario.builder().id(1L).build();
        EventoBaseDTO<LoteInventario> evento = new EventoBaseDTO<>();
        evento.setPayload(lote);

        when(motorPreciosService.calcularPrecioDinamico(lote)).thenThrow(new RuntimeException("Error"));

        loteEventConsumer.consumeLoteCreado(evento);

        verify(motorPreciosService).calcularPrecioDinamico(lote);
        // Exception is caught inside, so no rethrow
    }
}
