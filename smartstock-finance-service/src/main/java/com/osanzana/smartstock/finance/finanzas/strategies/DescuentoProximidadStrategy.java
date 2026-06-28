package com.osanzana.smartstock.finance.finanzas.strategies;

import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import com.osanzana.smartstock.finance.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.finance.core.repositories.LoteRepository;
import com.osanzana.smartstock.finance.shared.dto.events.AlertaDescuentoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class DescuentoProximidadStrategy implements CalculoPrecioStrategy {

    private final LoteRepository loteRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public boolean esAplicable(LoteInventario lote, ReglaDepreciacion regla) {
        return regla.getDiasCriticosMin() != null && regla.getDiasCriticosMin() > 0;
    }

    @Override
    public boolean aplicar(LoteInventario lote, ReglaDepreciacion regla) {
        long diasParaVencimiento = ChronoUnit.DAYS.between(LocalDate.now(), lote.getFechaVencimiento());

        if (diasParaVencimiento <= regla.getDiasCriticosMin()) {
            log.info("[Strategy] Criterio de proximidad cumplido: {} días para vencimiento <= {} días críticos.",
                    diasParaVencimiento, regla.getDiasCriticosMin());

            BigDecimal descuento = regla.getPorcentajeDescuento().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal precioConDescuento = lote.getPrecioDinamico().multiply(BigDecimal.ONE.subtract(descuento));
            
            lote.setPrecioDinamico(precioConDescuento);
            loteRepository.save(lote);

            log.info("[Strategy] Descuento de proximidad aplicado: {}%", regla.getPorcentajeDescuento());

            AlertaDescuentoEvent evento = AlertaDescuentoEvent.builder()
                    .loteId(lote.getId())
                    .productoNombre(lote.getProducto().getNombre())
                    .porcentajeDescuento(regla.getPorcentajeDescuento())
                    .nuevoPrecio(precioConDescuento)
                    .comercioId(lote.getComercio().getId())
                    .build();

            kafkaTemplate.send("alerta-etiqueta", String.valueOf(lote.getId()), evento);
            return true;
        }
        
        return false;
    }
}
