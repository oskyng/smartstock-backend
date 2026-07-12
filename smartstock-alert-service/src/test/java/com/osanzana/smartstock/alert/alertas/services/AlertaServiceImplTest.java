package com.osanzana.smartstock.alert.alertas.services;

import com.osanzana.smartstock.alert.alertas.entities.AlertaAccion;
import com.osanzana.smartstock.alert.alertas.repositories.AlertaAccionRepository;
import com.osanzana.smartstock.alert.alertas.stream.AlertaEventProducer;
import com.osanzana.smartstock.alert.core.entities.*;
import com.osanzana.smartstock.alert.core.repositories.LoteRepository;
import com.osanzana.smartstock.alert.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import com.osanzana.smartstock.alert.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertaServiceImplTest {

    @Mock
    private AlertaAccionRepository alertaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private LoteRepository loteRepository;
    @Mock
    private AlertaEventProducer alertaEventProducer;

    @InjectMocks
    private AlertaServiceImpl alertaService;

    private LoteInventario lote;
    private ReglaDepreciacion regla;
    private Usuario reponedor;
    private AlertaAccion alerta;

    @BeforeEach
    void setUp() {
        Comercio comercio = Comercio.builder().id(1L).build();
        Producto producto = Producto.builder().id(1L).nombre("Producto Test").build();
        lote = LoteInventario.builder()
                .id(1L)
                .comercio(comercio)
                .producto(producto)
                .fechaVencimiento(LocalDate.now().plusDays(5))
                .build();
        
        reponedor = Usuario.builder()
                .id(2L)
                .rol(Rol.builder().nombre("REPONEDOR_SALA").build())
                .build();

        regla = ReglaDepreciacion.builder()
                .id(1L)
                .porcentajeDescuento(new BigDecimal("10"))
                .gerente(Usuario.builder().id(3L).build())
                .build();

        alerta = AlertaAccion.builder()
                .id(1L)
                .lote(lote)
                .comercio(comercio)
                .descripcionAlerta("Test")
                .estadoAlerta("PENDIENTE")
                .usuarioAsignado(reponedor)
                .build();
    }

    @Test
    void listarPendientesPorComercio_Success() {
        when(alertaRepository.findByComercioIdAndEstadoAlerta(1L, "PENDIENTE"))
                .thenReturn(Collections.singletonList(alerta));

        List<AlertaResponseDTO> result = alertaService.listarPendientesPorComercio(1L);

        assertFalse(result.isEmpty());
        assertEquals("PENDIENTE", result.get(0).getEstado());
    }

    @Test
    void generarAlertaDescuento_Success() {
        when(usuarioRepository.findByRolNombreAndActivoAndComercioId("REPONEDOR_SALA", 1, 1L))
                .thenReturn(Collections.singletonList(reponedor));

        alertaService.generarAlertaDescuento(lote, regla);

        verify(alertaRepository).save(any(AlertaAccion.class));
    }

    @Test
    void generarAlertaDescuento_NoReponedor() {
        when(usuarioRepository.findByRolNombreAndActivoAndComercioId(anyString(), anyInt(), anyLong()))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> alertaService.generarAlertaDescuento(lote, regla));
    }

    @Test
    void atenderAlerta_Success() {
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alerta));

        alertaService.atenderAlerta(1L);

        assertEquals("ATENDIDA", alerta.getEstadoAlerta());
        assertNotNull(alerta.getFechaAtencion());
        verify(alertaRepository).save(alerta);
    }

    @Test
    void procesarEscalamientoSLA_Success() {
        when(alertaRepository.findByEstadoAlertaAndFechaLimiteAtencionBefore(eq("PENDIENTE"), any()))
                .thenReturn(Collections.singletonList(alerta));

        alertaService.procesarEscalamientoSLA();

        assertEquals("ESCALADA", alerta.getEstadoAlerta());
        verify(alertaRepository).save(alerta);
    }
}
