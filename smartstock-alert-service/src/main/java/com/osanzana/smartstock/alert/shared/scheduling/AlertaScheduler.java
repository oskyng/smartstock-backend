package com.osanzana.smartstock.alert.shared.scheduling;

import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertaScheduler {

    private final AlertaService alertaService;

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancarReconciliarLotesSinAlerta() {
        log.info("[STARTUP] Servicio disponible: reconciliando alertas de vencimiento pendientes...");
        try {
            alertaService.reconciliarLotesSinAlerta();
        } catch (Exception e) {
            log.error("[STARTUP] Error al reconciliar alertas de vencimiento: {}", e.getMessage());
        }
    }

    /**
     * Tarea periódica que corre cada 15 minutos para detectar alertas
     * PENDIENTES cuya fecha límite haya expirado y escalarlas automáticamente.
     */
    @Scheduled(fixedRate = 900000) // 15 minutos
    public void ejecutarEscalamientoSLA() {
        log.info("[SCHEDULED] Ejecutando auditoría automática de SLA...");
        alertaService.procesarEscalamientoSLA();
    }

    @Scheduled(fixedRate = 1800000, initialDelay = 1800000) // cada 30 minutos, empezando en 30 min
    public void ejecutarReconciliacionLotesSinAlerta() {
        log.info("[SCHEDULED] Ejecutando reconciliación periódica de alertas de vencimiento...");
        alertaService.reconciliarLotesSinAlerta();
    }
}
