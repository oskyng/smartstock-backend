package com.osanzana.smartstock.bff.stream;

import com.osanzana.smartstock.bff.dto.AlertaEscaladaEvent;
import com.osanzana.smartstock.bff.dto.EventoBaseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
@Slf4j
public class AuditStreamService {

    private final Sinks.Many<AlertaEscaladaEvent> sink;

    public AuditStreamService() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    @KafkaListener(topics = "alerta-escalada", groupId = "smartstock-bff-audit-group")
    public void consumeAlertaEscalada(EventoBaseDTO<AlertaEscaladaEvent> evento) {
        log.info("[STREAM] Recibida alerta escalada para producto: {}", evento.getPayload().getProductoNombre());
        sink.tryEmitNext(evento.getPayload());
    }

    public Flux<AlertaEscaladaEvent> getAuditStream() {
        return sink.asFlux();
    }
}
