package com.osanzana.smartstock.finance.finanzas.stream;

import com.osanzana.smartstock.finance.finanzas.services.MotorPreciosService;
import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import com.osanzana.smartstock.finance.shared.dto.EventoBaseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoteEventConsumer {

    private final MotorPreciosService motorPreciosService;

    @KafkaListener(topics = "lote-creado", groupId = "smartstock-group")
    @Transactional
    public void consumeLoteCreado(EventoBaseDTO<LoteInventario> evento) {
        log.info("[KAFKA] Evento LOTE_CREADO recibido. ID Evento: {}", 
                evento.getIdEvento());

        LoteInventario lote = evento.getPayload();
        
        if (lote == null) {
            log.error("El payload del evento está vacío.");
            return;
        }

        try {
            log.info("Iniciando procesamiento de precio dinámico para lote: {}", lote.getId());
            boolean accionRequerida = motorPreciosService.calcularPrecioDinamico(lote);
            
            if (accionRequerida) {
                log.info("Procesamiento completado: Se requiere acción física en sala para el lote {}", lote.getId());
            } else {
                log.info("Procesamiento completado: No se requiere acción adicional para el lote {}", lote.getId());
            }
        } catch (Exception e) {
            log.error("Error al procesar el precio dinámico del lote {}", lote.getId(), e);
            // En un entorno productivo, aquí se podría enviar a un Dead Letter Topic
        }
    }
}
