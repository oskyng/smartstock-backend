package com.osanzana.smartstock.alert.alertas.stream;

import com.osanzana.smartstock.alert.shared.dto.EventoBaseDTO;
import com.osanzana.smartstock.alert.shared.dto.events.AlertaEscaladaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAlertaEscalada(AlertaEscaladaEvent payload) {
        EventoBaseDTO<AlertaEscaladaEvent> evento = EventoBaseDTO.<AlertaEscaladaEvent>builder()
                .idEvento(UUID.randomUUID().toString())
                .tipoEvento("ALERTA_ESCALADA")
                .fechaEvento(LocalDateTime.now())
                .payload(payload)
                .build();

        log.info("[EVENTO] Publicando ALERTA_ESCALADA para Alerta ID: {}", payload.getAlertaId());
        kafkaTemplate.send("alerta-escalada", String.valueOf(payload.getAlertaId()), evento);
    }
}
