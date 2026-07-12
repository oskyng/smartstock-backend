package com.osanzana.smartstock.alert.alertas.stream;

import com.osanzana.smartstock.alert.alertas.entities.AlertaAccion;
import com.osanzana.smartstock.alert.alertas.repositories.AlertaAccionRepository;
import com.osanzana.smartstock.alert.core.entities.LoteInventario;
import com.osanzana.smartstock.alert.core.entities.Usuario;
import com.osanzana.smartstock.alert.core.repositories.LoteRepository;
import com.osanzana.smartstock.alert.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import com.osanzana.smartstock.alert.shared.dto.EventoBaseDTO;
import com.osanzana.smartstock.alert.shared.dto.events.AlertaDescuentoEvent;
import com.osanzana.smartstock.alert.shared.dto.events.LoteEventDTO;
import com.osanzana.smartstock.alert.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertaEventConsumer {

    private final AlertaAccionRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteRepository loteRepository;
    private final AlertaService alertaService;

    @KafkaListener(topics = "lote-creado", groupId = "smartstock-alert-group")
    @Transactional
    public void consumeLoteCreado(EventoBaseDTO<LoteEventDTO> evento) {
        log.info("[KAFKA] Recibido LOTE_CREADO para producto: {}", evento.getPayload().getProductoNombre());
        alertaService.procesarNuevoLote(evento.getPayload());
    }

    @KafkaListener(topics = "alerta-etiqueta", groupId = "smartstock-alert-group")
    @Transactional
    public void consumeAlertaDescuento(AlertaDescuentoEvent evento) {
        log.info("[KAFKA] Recibida solicitud de alerta para lote: {}", evento.getLoteId());

        try {
            LoteInventario lote = loteRepository.findById(evento.getLoteId())
                    .orElseThrow(() -> new BusinessException("Lote no encontrado para alerta: " + evento.getLoteId()));

            // Buscar un Reponedor del mismo comercio
            Usuario reponedor = usuarioRepository.findByRolNombreAndActivoAndComercioId("REPONEDOR_SALA", 1, evento.getComercioId())
                    .stream().findFirst()
                    .orElseThrow(() -> new BusinessException("No se encontró un REPONEDOR_SALA activo para el comercio ID: " + evento.getComercioId()));

            // En este flujo desacoplado, podríamos no tener la ReglaDepreciacion completa, 
            // pero tenemos los datos necesarios en el evento.
            
            String descripcion = String.format("APLICAR PRECIO DINÁMICO: Descuento del %s%% activo para %s. Nuevo precio: %s",
                    evento.getPorcentajeDescuento(), evento.getProductoNombre(), evento.getNuevoPrecio());

            AlertaAccion alerta = AlertaAccion.builder()
                    .lote(lote)
                    .comercio(lote.getComercio())
                    .descripcionAlerta(descripcion)
                    .estadoAlerta("PENDIENTE")
                    .usuarioAsignado(reponedor)
                    .usuarioSupervisor(null) // Podría asignarse por defecto si es necesario
                    .fechaLimiteAtencion(LocalDateTime.now().plusHours(24))
                    .build();

            alertaRepository.save(alerta);
            log.info("Alerta creada con éxito desde evento Kafka para lote {}", lote.getId());
            
        } catch (Exception e) {
            log.error("Error al procesar evento de alerta: {}", e.getMessage());
        }
    }
}
