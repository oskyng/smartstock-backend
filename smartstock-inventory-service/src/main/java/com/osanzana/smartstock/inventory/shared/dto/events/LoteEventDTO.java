package com.osanzana.smartstock.inventory.shared.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteEventDTO {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private LocalDate fechaVencimiento;
    private BigDecimal precioDinamico;
    private Long comercioId;
}
