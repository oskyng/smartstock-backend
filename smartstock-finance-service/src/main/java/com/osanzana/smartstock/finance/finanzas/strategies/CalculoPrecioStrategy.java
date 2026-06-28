package com.osanzana.smartstock.finance.finanzas.strategies;

import com.osanzana.smartstock.finance.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.finance.core.entities.LoteInventario;

public interface CalculoPrecioStrategy {
    /**
     * Determina si esta estrategia es aplicable para el lote y la regla dados.
     */
    boolean esAplicable(LoteInventario lote, ReglaDepreciacion regla);

    /**
     * Ejecuta el cálculo del precio dinámico y retorna true si se aplicó un cambio que requiere alerta.
     */
    boolean aplicar(LoteInventario lote, ReglaDepreciacion regla);
}
