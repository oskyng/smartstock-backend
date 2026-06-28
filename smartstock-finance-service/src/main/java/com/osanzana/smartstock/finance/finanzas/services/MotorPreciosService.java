package com.osanzana.smartstock.finance.finanzas.services;

import com.osanzana.smartstock.finance.core.entities.LoteInventario;

public interface MotorPreciosService {
    /**
     * Calcula el precio dinámico de un lote basado en reglas de depreciación.
     * @param lote El lote a procesar.
     * @return true si se aplicó un descuento y se requiere acción en sala, false en caso contrario.
     */
    boolean calcularPrecioDinamico(LoteInventario lote);
}
