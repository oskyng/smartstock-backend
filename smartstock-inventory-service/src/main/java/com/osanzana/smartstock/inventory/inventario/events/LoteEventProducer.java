package com.osanzana.smartstock.inventory.inventario.events;

import com.osanzana.smartstock.inventory.core.entities.LoteInventario;
import com.osanzana.smartstock.inventory.shared.dto.EventoBaseDTO;
import com.osanzana.smartstock.inventory.shared.dto.events.LoteEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoteEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLoteCreado(LoteInventario lote) {
        LoteEventDTO payload = LoteEventDTO.builder()
                .id(lote.getId())
                .productoId(lote.getProducto().getId())
                .productoNombre(lote.getProducto().getNombre())
                .cantidad(lote.getCantidadActual())
                .fechaVencimiento(lote.getFechaVencimiento())
                .precioDinamico(lote.getPrecioDinamico())
                .comercioId(lote.getComercio().getId())
                .build();

        EventoBaseDTO<LoteEventDTO> evento = EventoBaseDTO.<LoteEventDTO>builder()
                .idEvento(UUID.randomUUID().toString())
                .tipoEvento("LOTE_CREADO")
                .fechaEvento(LocalDateTime.now())
                .payload(payload)
                .build();

        log.info("[EVENTO] Publicando LOTE_CREADO para Lote ID: {}", lote.getId());
        kafkaTemplate.send("lote-creado", String.valueOf(lote.getId()), evento);
    }
}
