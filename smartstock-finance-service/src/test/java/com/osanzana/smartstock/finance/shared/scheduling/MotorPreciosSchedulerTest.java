package com.osanzana.smartstock.finance.shared.scheduling;

import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import com.osanzana.smartstock.finance.core.repositories.LoteRepository;
import com.osanzana.smartstock.finance.finanzas.services.MotorPreciosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorPreciosSchedulerTest {

    @Mock
    private LoteRepository loteRepository;
    @Mock
    private MotorPreciosService motorPreciosService;

    @InjectMocks
    private MotorPreciosScheduler scheduler;

    private LoteInventario conStock;
    private LoteInventario sinStock;

    @BeforeEach
    void setUp() {
        conStock = LoteInventario.builder().id(1L).cantidadActual(10).build();
        sinStock = LoteInventario.builder().id(2L).cantidadActual(0).build();
    }

    @Test
    void reevaluarPreciosPeriodicamente_SoloProcesaLotesConStock() {
        when(loteRepository.findAll()).thenReturn(Arrays.asList(conStock, sinStock));
        when(motorPreciosService.calcularPrecioDinamico(conStock)).thenReturn(true);

        scheduler.reevaluarPreciosPeriodicamente();

        verify(motorPreciosService).calcularPrecioDinamico(conStock);
        verify(motorPreciosService, never()).calcularPrecioDinamico(sinStock);
    }

    @Test
    void reevaluarPreciosPeriodicamente_SinLotes_NoHaceNada() {
        when(loteRepository.findAll()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> scheduler.reevaluarPreciosPeriodicamente());

        verify(motorPreciosService, never()).calcularPrecioDinamico(any());
    }

    @Test
    void reevaluarPreciosPeriodicamente_UnLoteFalla_ContinuaConLosDemas() {
        LoteInventario otroConStock = LoteInventario.builder().id(3L).cantidadActual(5).build();
        List<LoteInventario> lotes = Arrays.asList(conStock, otroConStock);
        when(loteRepository.findAll()).thenReturn(lotes);
        when(motorPreciosService.calcularPrecioDinamico(conStock)).thenThrow(new RuntimeException("DB error"));
        when(motorPreciosService.calcularPrecioDinamico(otroConStock)).thenReturn(true);

        assertDoesNotThrow(() -> scheduler.reevaluarPreciosPeriodicamente());

        verify(motorPreciosService).calcularPrecioDinamico(conStock);
        verify(motorPreciosService).calcularPrecioDinamico(otroConStock);
    }

    @Test
    void alArrancarReevaluarPrecios_NoPropagaExcepcion() {
        when(loteRepository.findAll()).thenThrow(new RuntimeException("DB no disponible"));

        assertDoesNotThrow(() -> scheduler.alArrancarReevaluarPrecios());
    }
}
