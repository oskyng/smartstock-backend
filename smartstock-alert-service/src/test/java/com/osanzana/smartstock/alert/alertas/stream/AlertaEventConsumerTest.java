package com.osanzana.smartstock.alert.alertas.stream;

import com.osanzana.smartstock.alert.alertas.entities.AlertaAccion;
import com.osanzana.smartstock.alert.alertas.repositories.AlertaAccionRepository;
import com.osanzana.smartstock.alert.core.entities.Comercio;
import com.osanzana.smartstock.alert.core.entities.LoteInventario;
import com.osanzana.smartstock.alert.core.entities.Usuario;
import com.osanzana.smartstock.alert.core.repositories.LoteRepository;
import com.osanzana.smartstock.alert.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.alert.shared.dto.events.AlertaDescuentoEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaEventConsumerTest {

    @Mock private AlertaAccionRepository alertaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LoteRepository loteRepository;

    @InjectMocks
    private AlertaEventConsumer consumer;

    private AlertaDescuentoEvent evento;
    private LoteInventario lote;
    private Usuario reponedor;

    @BeforeEach
    void setUp() {
        evento = AlertaDescuentoEvent.builder()
                .loteId(1L)
                .productoNombre("Test Prod")
                .porcentajeDescuento(new BigDecimal("10"))
                .nuevoPrecio(new BigDecimal("90"))
                .comercioId(1L)
                .build();

        lote = LoteInventario.builder()
                .id(1L)
                .comercio(Comercio.builder().id(1L).build())
                .build();

        reponedor = Usuario.builder().id(1L).build();
    }

    @Test
    void consumeAlertaDescuento_Success() {
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        when(usuarioRepository.findByRolNombreAndActivoAndComercioId("REPONEDOR_SALA", 1, 1L))
                .thenReturn(Collections.singletonList(reponedor));

        consumer.consumeAlertaDescuento(evento);

        verify(alertaRepository).save(any(AlertaAccion.class));
    }

    @Test
    void consumeAlertaDescuento_LoteNotFound() {
        when(loteRepository.findById(1L)).thenReturn(Optional.empty());

        consumer.consumeAlertaDescuento(evento);

        verify(alertaRepository, never()).save(any());
    }
}
