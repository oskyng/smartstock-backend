package com.osanzana.smartstock.alert.shared.scheduling;

import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlertaSchedulerTest {

    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private AlertaScheduler alertaScheduler;

    @Test
    void alArrancarReconciliarLotesSinAlerta_DelegaAlServicio() {
        alertaScheduler.alArrancarReconciliarLotesSinAlerta();

        verify(alertaService).reconciliarLotesSinAlerta();
    }

    @Test
    void alArrancarReconciliarLotesSinAlerta_NoPropagaExcepcion() {
        // Una excepción aquí no debe tumbar el arranque del servicio (ApplicationReadyEvent).
        doThrow(new RuntimeException("DB no disponible")).when(alertaService).reconciliarLotesSinAlerta();

        assertDoesNotThrow(() -> alertaScheduler.alArrancarReconciliarLotesSinAlerta());
    }

    @Test
    void ejecutarEscalamientoSLA_DelegaAlServicio() {
        alertaScheduler.ejecutarEscalamientoSLA();

        verify(alertaService).procesarEscalamientoSLA();
    }

    @Test
    void ejecutarReconciliacionLotesSinAlerta_DelegaAlServicio() {
        alertaScheduler.ejecutarReconciliacionLotesSinAlerta();

        verify(alertaService).reconciliarLotesSinAlerta();
    }
}
