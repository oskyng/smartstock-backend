package com.osanzana.smartstock.bff.stream;

import com.osanzana.smartstock.bff.dto.AlertaEscaladaEvent;
import com.osanzana.smartstock.bff.dto.EventoBaseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

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

    /**
     * Envía un heartbeat inmediato (delay cero) y luego cada 15s: fuerza a que el servidor
     * confirme (flush) la respuesta HTTP apenas el cliente se suscribe, en vez de mantenerla
     * sin encabezados enviados hasta la primera alerta real (que puede tardar horas en ocurrir).
     * Los comentarios SSE (":heartbeat") no llevan "data:", así que el cliente los ignora sin
     * tratarlos como eventos de negocio.
     */
    public Flux<ServerSentEvent<AlertaEscaladaEvent>> getAuditStream(Long comercioId) {
        Flux<ServerSentEvent<AlertaEscaladaEvent>> heartbeat = Flux.interval(Duration.ZERO, Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.<AlertaEscaladaEvent>builder().comment("heartbeat").build());

        Flux<ServerSentEvent<AlertaEscaladaEvent>> eventos = sink.asFlux()
                .filter(evento -> evento.getComercioId() != null && evento.getComercioId().equals(comercioId))
                .map(evento -> ServerSentEvent.builder(evento).build());

        return Flux.merge(heartbeat, eventos);
    }
}
