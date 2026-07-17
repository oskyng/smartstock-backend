package com.osanzana.smartstock.bff.stream;

import com.osanzana.smartstock.bff.dto.AlertaEscaladaEvent;
import com.osanzana.smartstock.bff.dto.EventoBaseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

class AuditStreamServiceTest {

    @Test
    void getAuditStream_SoloEmiteEventosDelComercioSolicitado() {
        AuditStreamService service = new AuditStreamService();

        AlertaEscaladaEvent eventoComercio1 = AlertaEscaladaEvent.builder()
                .alertaId(1L)
                .productoNombre("Producto Comercio 1")
                .comercioId(1L)
                .build();
        AlertaEscaladaEvent eventoComercio2 = AlertaEscaladaEvent.builder()
                .alertaId(2L)
                .productoNombre("Producto Comercio 2")
                .comercioId(2L)
                .build();

        Flux<ServerSentEvent<AlertaEscaladaEvent>> stream = service.getAuditStream(1L)
                .filter(sse -> sse.data() != null); // descarta heartbeats para simplificar la aserción

        StepVerifier.create(stream)
                .then(() -> {
                    service.consumeAlertaEscalada(wrap(eventoComercio2));
                    service.consumeAlertaEscalada(wrap(eventoComercio1));
                })
                .expectNextMatches(sse -> sse.data().getAlertaId().equals(1L))
                .thenCancel()
                .verify(Duration.ofSeconds(2));
    }

    private EventoBaseDTO<AlertaEscaladaEvent> wrap(AlertaEscaladaEvent payload) {
        EventoBaseDTO<AlertaEscaladaEvent> evento = new EventoBaseDTO<>();
        evento.setPayload(payload);
        return evento;
    }
}
