package com.osanzana.smartstock.finance.finanzas.strategies;

import com.osanzana.smartstock.finance.core.entities.*;
import com.osanzana.smartstock.finance.core.repositories.LoteRepository;
import com.osanzana.smartstock.finance.shared.dto.events.AlertaDescuentoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DescuentoProximidadStrategyTest {

    @Mock
    private LoteRepository loteRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private DescuentoProximidadStrategy strategy;

    private LoteInventario lote;
    private ReglaDepreciacion regla;

    @BeforeEach
    void setUp() {
        lote = LoteInventario.builder()
                .id(1L)
                .producto(Producto.builder().nombre("Producto Test").precioBase(new BigDecimal("100.00")).build())
                .comercio(Comercio.builder().id(1L).build())
                .precioDinamico(new BigDecimal("100.00"))
                .fechaVencimiento(LocalDate.now().plusDays(5))
                .build();

        regla = ReglaDepreciacion.builder()
                .diasCriticosMin(7)
                .porcentajeDescuento(new BigDecimal("20.00"))
                .build();
    }

    @Test
    void esAplicable_True() {
        assertTrue(strategy.esAplicable(lote, regla));
    }

    @Test
    void esAplicable_False_Null() {
        regla.setDiasCriticosMin(null);
        assertFalse(strategy.esAplicable(lote, regla));
    }

    @Test
    void aplicar_Success() {
        boolean resultado = strategy.aplicar(lote, regla);

        assertTrue(resultado);
        assertEquals(new BigDecimal("80.00"), lote.getPrecioDinamico());
        verify(loteRepository).save(lote);
        verify(kafkaTemplate).send(eq("alerta-etiqueta"), anyString(), any(AlertaDescuentoEvent.class));
    }

    @Test
    void aplicar_False_TooManyDays() {
        lote.setFechaVencimiento(LocalDate.now().plusDays(10));
        boolean resultado = strategy.aplicar(lote, regla);

        assertFalse(resultado);
        assertEquals(new BigDecimal("100.00"), lote.getPrecioDinamico());
        verify(loteRepository, never()).save(any());
    }

    @Test
    void aplicar_EsIdempotente_NoRecomponeDescuentoYaAplicado() {
        // Simula que el lote ya tiene el descuento del 20% aplicado (80.00, calculado sobre
        // los mismos 100.00 de precioBase). Reevaluar la misma regla no debe volver a descontar
        // sobre el precio ya descontado (lo que daría 64.00 si se compusiera incorrectamente).
        lote.setPrecioDinamico(new BigDecimal("80.00"));

        boolean resultado = strategy.aplicar(lote, regla);

        assertFalse(resultado);
        assertEquals(new BigDecimal("80.00"), lote.getPrecioDinamico());
        verify(loteRepository, never()).save(any());
    }

    @Test
    void aplicar_ReglaMasAgresiva_ProfundizaElDescuento() {
        // El lote ya tiene el descuento del 20% aplicado; una regla más agresiva (50%) debe
        // profundizar el precio, ya que el resultado (50.00) es menor al actual (80.00).
        lote.setPrecioDinamico(new BigDecimal("80.00"));
        regla.setPorcentajeDescuento(new BigDecimal("50.00"));

        boolean resultado = strategy.aplicar(lote, regla);

        assertTrue(resultado);
        assertEquals(new BigDecimal("50.00"), lote.getPrecioDinamico());
        verify(loteRepository).save(lote);
    }
}
