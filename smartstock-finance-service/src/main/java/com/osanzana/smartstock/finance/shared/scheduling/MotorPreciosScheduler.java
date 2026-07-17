package com.osanzana.smartstock.finance.shared.scheduling;

import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import com.osanzana.smartstock.finance.core.repositories.LoteRepository;
import com.osanzana.smartstock.finance.finanzas.services.MotorPreciosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * El precio dinámico de un lote solo se recalculaba una vez, al momento de crearse (evento Kafka
 * lote-creado). Si un lote no calificaba para ningún descuento al crearse pero luego cruza el
 * umbral de días críticos con el simple paso del tiempo, nada volvía a evaluarlo — el precio
 * quedaba desactualizado indefinidamente. Este componente reevalúa periódicamente todos los
 * lotes con stock, cubriendo tanto ese caso como cualquier downtime del servicio o de Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MotorPreciosScheduler {

    private final LoteRepository loteRepository;
    private final MotorPreciosService motorPreciosService;

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancarReevaluarPrecios() {
        log.info("[STARTUP] Servicio disponible: reevaluando precios dinámicos de lotes con stock...");
        try {
            reevaluarTodosLosLotes();
        } catch (Exception e) {
            log.error("[STARTUP] Error al reevaluar precios dinámicos: {}", e.getMessage());
        }
    }

    /** initialDelay evita que la primera corrida periódica se dispare junto con la de arranque. */
    @Scheduled(fixedRate = 1800000, initialDelay = 1800000) // cada 30 minutos, empezando en 30 min
    public void reevaluarPreciosPeriodicamente() {
        log.info("[SCHEDULED] Reevaluando precios dinámicos de lotes con stock...");
        reevaluarTodosLosLotes();
    }

    private void reevaluarTodosLosLotes() {
        List<LoteInventario> lotes = loteRepository.findAll().stream()
                .filter(lote -> lote.getCantidadActual() != null && lote.getCantidadActual() > 0)
                .toList();

        int actualizados = 0;
        for (LoteInventario lote : lotes) {
            try {
                if (motorPreciosService.calcularPrecioDinamico(lote)) {
                    actualizados++;
                }
            } catch (Exception e) {
                log.error("Error al reevaluar precio dinámico del lote {}: {}", lote.getId(), e.getMessage());
            }
        }

        log.info("[PRECIOS] Reevaluación finalizada: {} lote(s) actualizado(s) de {} revisado(s).",
                actualizados, lotes.size());
    }
}
