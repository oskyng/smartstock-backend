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
                .producto(Producto.builder().nombre("Producto Test").build())
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
        assertEquals(new BigDecimal("80.0000"), lote.getPrecioDinamico());
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
}
