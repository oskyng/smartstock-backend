package com.osanzana.smartstock.finance.finanzas.services;

import com.osanzana.smartstock.finance.core.entities.*;
import com.osanzana.smartstock.finance.finanzas.repositories.ReglaDepreciacionRepository;
import com.osanzana.smartstock.finance.finanzas.strategies.CalculoPrecioStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MotorPreciosServiceImplTest {

    @Mock
    private ReglaDepreciacionRepository reglaRepository;

    @Mock
    private CalculoPrecioStrategy estrategia;

    private MotorPreciosServiceImpl motorPreciosService;

    private LoteInventario lote;
    private ReglaDepreciacion regla;

    @BeforeEach
    void setUp() {
        motorPreciosService = new MotorPreciosServiceImpl(reglaRepository, Collections.singletonList(estrategia));

        Categoria cat = new Categoria();
        cat.setId(1L);
        cat.setNombre("Test");

        Producto prod = Producto.builder().id(1L).categoria(cat).build();
        Comercio com = Comercio.builder().id(1L).build();
        lote = LoteInventario.builder().id(1L).producto(prod).comercio(com).build();
        regla = ReglaDepreciacion.builder().id(1L).build();
    }

    @Test
    void calcularPrecioDinamico_NoReglas() {
        when(reglaRepository.findByCategoriaIdAndComercioIdAndActiva(anyLong(), anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());

        boolean result = motorPreciosService.calcularPrecioDinamico(lote);

        assertFalse(result);
        verify(estrategia, never()).aplicar(any(), any());
    }

    @Test
    void calcularPrecioDinamico_ConReglasYAplicable() {
        when(reglaRepository.findByCategoriaIdAndComercioIdAndActiva(anyLong(), anyLong(), anyInt()))
                .thenReturn(Collections.singletonList(regla));
        when(estrategia.esAplicable(lote, regla)).thenReturn(true);
        when(estrategia.aplicar(lote, regla)).thenReturn(true);

        boolean result = motorPreciosService.calcularPrecioDinamico(lote);

        assertTrue(result);
        verify(estrategia).aplicar(lote, regla);
    }

    @Test
    void calcularPrecioDinamico_ConReglasNoAplicable() {
        when(reglaRepository.findByCategoriaIdAndComercioIdAndActiva(anyLong(), anyLong(), anyInt()))
                .thenReturn(Collections.singletonList(regla));
        when(estrategia.esAplicable(lote, regla)).thenReturn(false);

        boolean result = motorPreciosService.calcularPrecioDinamico(lote);

        assertFalse(result);
        verify(estrategia, never()).aplicar(any(), any());
    }

    @Test
    void calcularPrecioDinamico_ExceptionHandled() {
        when(reglaRepository.findByCategoriaIdAndComercioIdAndActiva(anyLong(), anyLong(), anyInt()))
                .thenReturn(Collections.singletonList(regla));
        when(estrategia.esAplicable(lote, regla)).thenReturn(true);
        when(estrategia.aplicar(lote, regla)).thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class, () -> motorPreciosService.calcularPrecioDinamico(lote));
    }
}
