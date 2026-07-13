package com.osanzana.smartstock.inventory.inventario.events;

import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.entities.LoteInventario;
import com.osanzana.smartstock.inventory.core.entities.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoteEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private LoteEventProducer loteEventProducer;

    @Test
    void publishLoteCreado_Success() {
        Producto producto = Producto.builder().id(1L).nombre("Test").build();
        Comercio comercio = Comercio.builder().id(1L).build();
        LoteInventario lote = LoteInventario.builder()
                .id(1L)
                .producto(producto)
                .comercio(comercio)
                .cantidadActual(10)
                .fechaVencimiento(LocalDate.now().plusDays(10))
                .precioDinamico(new BigDecimal("100"))
                .build();

        loteEventProducer.publishLoteCreado(lote);

        verify(kafkaTemplate).send(eq("lote-creado"), eq("1"), any());
    }
}
