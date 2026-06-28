package com.osanzana.smartstock.finance.finanzas.services;

import com.osanzana.smartstock.finance.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.finance.finanzas.repositories.ReglaDepreciacionRepository;
import com.osanzana.smartstock.finance.finanzas.strategies.CalculoPrecioStrategy;
import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MotorPreciosServiceImpl implements MotorPreciosService {

    private final ReglaDepreciacionRepository reglaRepository;
    private final List<CalculoPrecioStrategy> estrategias;

    @Override
    @Transactional
    public boolean calcularPrecioDinamico(LoteInventario lote) {
        log.info("Iniciando cálculo de precio dinámico para el lote {} usando patrón Strategy", lote.getId());

        // Buscar reglas activas para la categoría del producto y comercio
        List<ReglaDepreciacion> reglas = reglaRepository.findByCategoriaIdAndComercioIdAndActiva(
                lote.getProducto().getCategoria().getId(),
                lote.getComercio().getId(),
                1
        );

        if (reglas.isEmpty()) {
            log.info("No se encontraron reglas de depreciación activas para la categoría {} en el comercio {}",
                    lote.getProducto().getCategoria().getNombre(), lote.getComercio().getId());
            return false;
        }

        // Procesar el lote con las estrategias disponibles que sean aplicables
        boolean accionRequerida = false;
        for (ReglaDepreciacion regla : reglas) {
            for (CalculoPrecioStrategy estrategia : estrategias) {
                if (estrategia.esAplicable(lote, regla)) {
                    boolean resultado = estrategia.aplicar(lote, regla);
                    if (resultado) {
                        accionRequerida = true;
                    }
                }
            }
        }

        return accionRequerida;
    }
}
