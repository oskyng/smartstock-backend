package com.osanzana.smartstock.alert.alertas.services;

import com.osanzana.smartstock.alert.alertas.entities.AlertaAccion;
import com.osanzana.smartstock.alert.alertas.repositories.AlertaAccionRepository;
import com.osanzana.smartstock.alert.alertas.stream.AlertaEventProducer;
import com.osanzana.smartstock.alert.core.entities.Usuario;
import com.osanzana.smartstock.alert.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.alert.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.alert.core.entities.LoteInventario;
import com.osanzana.smartstock.alert.core.repositories.LoteRepository;
import com.osanzana.smartstock.alert.shared.dto.events.AlertaEscaladaEvent;
import com.osanzana.smartstock.alert.shared.dto.events.LoteEventDTO;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import com.osanzana.smartstock.alert.shared.dto.response.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.alert.shared.exception.BusinessException;
import com.osanzana.smartstock.alert.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertaServiceImpl implements AlertaService {

    private final AlertaAccionRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteRepository loteRepository;
    private final AlertaEventProducer alertaEventProducer;

    @Value("${smartstock.services.finance.url:http://localhost:8084}")
    private String financeServiceUrl;

    private final RestClient restClient = RestClient.create();

    @Override
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPendientes() {
        log.info("Listando alertas PENDIENTES");
        return alertaRepository.findAll().stream()
                .filter(a -> "PENDIENTE".equals(a.getEstadoAlerta()))
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPorComercio(Long comercioId) {
        log.info("Listando alertas para el comercio {}", comercioId);
        return alertaRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaResponseDTO> listarPendientesPorComercio(Long comercioId) {
        log.info("Listando alertas PENDIENTES para el comercio {}", comercioId);
        return alertaRepository.findByComercioIdAndEstadoAlerta(comercioId, "PENDIENTE").stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void generarAlertaDescuento(LoteInventario lote, ReglaDepreciacion regla) {
        log.info("Generando alerta de descuento para lote {} en comercio {}", lote.getId(), lote.getComercio().getId());

        // Buscar un Reponedor del mismo comercio
        Usuario reponedor = usuarioRepository.findByRolNombreAndActivoAndComercioId("REPONEDOR_SALA", 1, lote.getComercio().getId())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("No se encontró un REPONEDOR_SALA activo para el comercio ID: " + lote.getComercio().getId()));

        // El supervisor es el gerente que creó la regla
        Usuario supervisor = regla.getGerente();

        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), lote.getFechaVencimiento());

        String descripcion = String.format("APLICAR PRECIO DINÁMICO: Descuento del %s%% activo por proximidad de vencimiento (Lote venciendo en %d días).",
                regla.getPorcentajeDescuento(), diasRestantes);

        AlertaAccion alerta = AlertaAccion.builder()
                .lote(lote)
                .reglaAplicada(regla)
                .comercio(lote.getComercio())
                .descripcionAlerta(descripcion)
                .estadoAlerta("PENDIENTE")
                .usuarioAsignado(reponedor)
                .usuarioSupervisor(supervisor)
                .fechaLimiteAtencion(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusHours(24))
                .build();

        alertaRepository.save(alerta);
        log.info("Alerta de acción en sala creada con éxito para el lote {}", lote.getId());
    }

    @Override
    @Transactional
    public void atenderAlerta(Long alertaId) {
        AlertaAccion alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada con ID: " + alertaId));
        
        alerta.setEstadoAlerta("ATENDIDA");
        alerta.setFechaAtencion(LocalDateTime.now());
        alertaRepository.save(alerta);
        log.info("Alerta {} marcada como ATENDIDA", alertaId);
    }

    @Override
    @Transactional
    public void procesarEscalamientoSLA() {
        log.info("Iniciando proceso de escalamiento de SLA para alertas vencidas...");
        List<AlertaAccion> expiradas = alertaRepository.findByEstadoAlertaAndFechaLimiteAtencionBefore("PENDIENTE", LocalDateTime.now());
        
        expiradas.forEach(alerta -> {
            log.warn("Escalando alerta {} por vencimiento de SLA", alerta.getId());
            alerta.setEstadoAlerta("ESCALADA");
            alertaRepository.save(alerta);

            // Notificar vía Kafka
            alertaEventProducer.publishAlertaEscalada(AlertaEscaladaEvent.builder()
                    .alertaId(alerta.getId())
                    .loteId(alerta.getLote().getId())
                    .productoNombre(alerta.getLote().getProducto().getNombre())
                    .comercioId(alerta.getComercio().getId())
                    .descripcion("Alerta escalada por incumplimiento de SLA (24h)")
                    .fechaEscalamiento(LocalDateTime.now())
                    .build());
        });
        
        if (!expiradas.isEmpty()) {
            log.info("Se escalaron {} alertas correctamente.", expiradas.size());
        }
    }

    @Override
    @Transactional
    public void procesarNuevoLote(LoteEventDTO loteEvent) {
        log.info("[LOGIC] Procesando nuevo lote {} para el comercio {}", loteEvent.getId(), loteEvent.getComercioId());

        try {
            // 1. Obtener reglas financieras para el comercio
            List<ReglaDepreciacionResponseDTO> reglas = restClient.get()
                    .uri(financeServiceUrl + "/api/v1/reglas-depreciacion")
                    .header("X-Comercio-ID", String.valueOf(loteEvent.getComercioId()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ReglaDepreciacionResponseDTO>>() {});

            if (reglas == null || reglas.isEmpty()) {
                log.info("No se encontraron reglas de depreciación activas para el comercio {}", loteEvent.getComercioId());
                return;
            }

            // 2. Buscar lote en la base de datos local
            LoteInventario lote = loteRepository.findById(loteEvent.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado localmente: " + loteEvent.getId()));

            // 3. Evaluar si aplica alguna regla por días críticos
            long diasParaVencer = ChronoUnit.DAYS.between(LocalDate.now(), lote.getFechaVencimiento());

            reglas.stream()
                    .filter(r -> r.getActiva() == 1 && diasParaVencer <= r.getDiasCriticosMin())
                    .findFirst()
                    .ifPresent(regla -> {
                        log.info("Aplicando regla de depreciación: {} días críticos, {}% descuento", 
                                regla.getDiasCriticosMin(), regla.getPorcentajeDescuento());
                        
                        // Crear alerta de acción
                        generarAlertaDescuentoDirecta(lote, regla);
                    });

        } catch (Exception e) {
            log.error("Error al procesar nuevo lote para alertas: {}", e.getMessage());
        }
    }

    private void generarAlertaDescuentoDirecta(LoteInventario lote, ReglaDepreciacionResponseDTO regla) {
        Usuario reponedor = usuarioRepository.findByRolNombreAndActivoAndComercioId("REPONEDOR_SALA", 1, lote.getComercio().getId())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("No se encontró un REPONEDOR_SALA activo para el comercio ID: " + lote.getComercio().getId()));

        String descripcion = String.format("APLICAR PRECIO DINÁMICO: Descuento del %s%% activo por proximidad de vencimiento.",
                regla.getPorcentajeDescuento());

        AlertaAccion alerta = AlertaAccion.builder()
                .lote(lote)
                .comercio(lote.getComercio())
                .descripcionAlerta(descripcion)
                .estadoAlerta("PENDIENTE")
                .usuarioAsignado(reponedor)
                .fechaLimiteAtencion(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES).plusHours(24))
                .build();

        alertaRepository.save(alerta);
        log.info("Alerta creada con éxito para el lote {}", lote.getId());
    }

    private AlertaResponseDTO mapToResponseDTO(AlertaAccion alerta) {
        return AlertaResponseDTO.builder()
                .id(alerta.getId())
                .mensaje(alerta.getDescripcionAlerta())
                .rolAsignado(alerta.getUsuarioAsignado().getRol().getNombre())
                .fechaLimite(alerta.getFechaLimiteAtencion())
                .estado(alerta.getEstadoAlerta())
                .fechaCreacion(alerta.getFechaNotificacion())
                .build();
    }
}
