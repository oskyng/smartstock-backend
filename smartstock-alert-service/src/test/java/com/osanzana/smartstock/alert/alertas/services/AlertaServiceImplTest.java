package com.osanzana.smartstock.alert.alertas.services;

import com.osanzana.smartstock.alert.alertas.entities.AlertaAccion;
import com.osanzana.smartstock.alert.alertas.repositories.AlertaAccionRepository;
import com.osanzana.smartstock.alert.alertas.stream.AlertaEventProducer;
import com.osanzana.smartstock.alert.core.entities.*;
import com.osanzana.smartstock.alert.core.repositories.LoteRepository;
import com.osanzana.smartstock.alert.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaAuditoriaResponseDTO;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import com.osanzana.smartstock.alert.shared.exception.BusinessException;
import com.osanzana.smartstock.alert.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.alert.shared.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AlertaServiceImpl alertaService;

    private LoteInventario lote;
    private ReglaDepreciacion regla;
    private Usuario reponedor;
    private AlertaAccion alerta;

    @BeforeEach
    void setUp() {
        Comercio comercio = Comercio.builder().id(1L).build();
        Producto producto = Producto.builder().id(1L).nombre("Producto Test").codigoBarra("7801234500019").build();
        lote = LoteInventario.builder()
                .id(1L)
                .comercio(comercio)
                .producto(producto)
                .fechaVencimiento(LocalDate.now().plusDays(5))
                .build();
        
        reponedor = Usuario.builder()
                .id(2L)
                .nombre("Diego")
                .apellido("Silva")
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
    void atenderAlerta_ATiempo_Success() {
        alerta.setFechaLimiteAtencion(LocalDateTime.now().plusHours(1));
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alerta));

        alertaService.atenderAlerta(1L, 1L);

        assertEquals("ATENDIDA_A_TIEMPO", alerta.getEstadoAlerta());
        assertNotNull(alerta.getFechaAtencion());
        verify(alertaRepository).save(alerta);
    }

    @Test
    void atenderAlerta_ConRetraso_Success() {
        alerta.setFechaLimiteAtencion(LocalDateTime.now().minusHours(1));
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alerta));

        alertaService.atenderAlerta(1L, 1L);

        assertEquals("ATENDIDA_CON_RETRASO", alerta.getEstadoAlerta());
        assertNotNull(alerta.getFechaAtencion());
        verify(alertaRepository).save(alerta);
    }

    @Test
    void atenderAlerta_DeOtroComercio_LanzaNotFound() {
        // La alerta existe pero pertenece a otro comercio: debe comportarse como si no existiera
        // (IDOR) en vez de permitir que un reponedor de otro comercio la marque como atendida.
        when(alertaRepository.findById(1L)).thenReturn(Optional.of(alerta));

        assertThrows(ResourceNotFoundException.class, () -> alertaService.atenderAlerta(1L, 99L));
        verify(alertaRepository, never()).save(any(AlertaAccion.class));
    }

    @Test
    void procesarEscalamientoSLA_Success() {
        when(alertaRepository.findByEstadoAlertaAndFechaLimiteAtencionBefore(eq("PENDIENTE"), any()))
                .thenReturn(Collections.singletonList(alerta));

        alertaService.procesarEscalamientoSLA();

        assertEquals("OMITIDA", alerta.getEstadoAlerta());
        verify(alertaRepository).save(alerta);
    }

    @Test
    void listarAuditoriaPorComercio_Success() {
        when(alertaRepository.findByComercioId(1L)).thenReturn(Collections.singletonList(alerta));

        List<AlertaAuditoriaResponseDTO> result = alertaService.listarAuditoriaPorComercio(1L);

        assertEquals(1, result.size());
        AlertaAuditoriaResponseDTO dto = result.get(0);
        assertEquals(1L, dto.getId());
        assertEquals(1L, dto.getLoteId());
        assertEquals("Producto Test", dto.getProductoNombre());
        assertEquals("7801234500019", dto.getCodigoBarra());
        assertEquals(2L, dto.getUsuarioAsignadoId());
        assertEquals("Diego Silva", dto.getUsuarioAsignadoNombre());
        assertEquals("PENDIENTE", dto.getEstadoAlerta());
        assertEquals(lote.getFechaVencimiento(), dto.getFechaVencimientoLote());
        assertFalse(dto.isLoteVencido());
    }

    @Test
    void listarAuditoriaPorComercio_SinAsignado() {
        alerta.setUsuarioAsignado(null);
        when(alertaRepository.findByComercioId(1L)).thenReturn(Collections.singletonList(alerta));

        List<AlertaAuditoriaResponseDTO> result = alertaService.listarAuditoriaPorComercio(1L);

        assertNull(result.get(0).getUsuarioAsignadoId());
        assertEquals("Sin asignar", result.get(0).getUsuarioAsignadoNombre());
    }

    @Test
    void listarAuditoriaPorComercio_LoteVencido_MarcaLoteVencidoTrue() {
        // Lote OMITIDA cuya fecha_vencimiento ya pasó: venció físicamente en góndola sin gestionarse.
        lote.setFechaVencimiento(LocalDate.now().minusDays(2));
        alerta.setEstadoAlerta("OMITIDA");
        when(alertaRepository.findByComercioId(1L)).thenReturn(Collections.singletonList(alerta));

        List<AlertaAuditoriaResponseDTO> result = alertaService.listarAuditoriaPorComercio(1L);

        assertTrue(result.get(0).isLoteVencido());
        assertEquals(lote.getFechaVencimiento(), result.get(0).getFechaVencimientoLote());
    }

    @Test
    void reconciliarLotesSinAlerta_SinLotes_NoHaceNada() {
        when(loteRepository.findAll()).thenReturn(Collections.emptyList());

        alertaService.reconciliarLotesSinAlerta();

        verify(alertaRepository, never()).save(any(AlertaAccion.class));
    }

    @Test
    void reconciliarLotesSinAlerta_TodosLosLotesYaTienenAlerta_NoConsultaReglas() {
        when(loteRepository.findAll()).thenReturn(Collections.singletonList(lote));
        when(alertaRepository.existsByLoteId(1L)).thenReturn(true);

        alertaService.reconciliarLotesSinAlerta();

        verify(alertaRepository, never()).save(any(AlertaAccion.class));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void reconciliarLotesSinAlerta_FinanceServiceNoDisponible_NoRompeNiGeneraAlertas() {
        // Simula un lote vencido (2 días atrás) sin alerta, típico caso de "downtime durante
        // los días previos al vencimiento". Como finance-service no está disponible en el test
        // (RestClient real sin servidor detrás), el guard de errores debe capturarlo y seguir
        // sin propagar la excepción ni guardar nada.
        lote.setFechaVencimiento(LocalDate.now().minusDays(2));
        when(loteRepository.findAll()).thenReturn(Collections.singletonList(lote));
        when(alertaRepository.existsByLoteId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> alertaService.reconciliarLotesSinAlerta());

        verify(alertaRepository, never()).save(any(AlertaAccion.class));
    }
}
