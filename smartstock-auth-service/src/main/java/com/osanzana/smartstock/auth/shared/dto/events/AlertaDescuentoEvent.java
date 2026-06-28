package com.osanzana.smartstock.auth.shared.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaDescuentoEvent {
    private Long loteId;
    private String productoNombre;
    private BigDecimal porcentajeDescuento;
    private BigDecimal nuevoPrecio;
    private Long comercioId;
}
