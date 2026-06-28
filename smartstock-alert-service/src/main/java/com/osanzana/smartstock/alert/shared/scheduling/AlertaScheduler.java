package com.osanzana.smartstock.alert.shared.scheduling;

import com.osanzana.smartstock.alert.alertas.services.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertaScheduler {

    private final AlertaService alertaService;

    /**
     * Tarea periódica que corre cada 15 minutos para detectar alertas 
     * PENDIENTES cuya fecha límite haya expirado y escalarlas automáticamente.
     */
    @Scheduled(fixedRate = 900000) // 15 minutos
    public void ejecutarEscalamientoSLA() {
        log.info("[SCHEDULED] Ejecutando auditoría automática de SLA...");
        alertaService.procesarEscalamientoSLA();
    }
}
