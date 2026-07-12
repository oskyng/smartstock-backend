package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LoteResponseDTO {
    private Long id;
    private String nombreProducto;
    private Integer cantidadActual;
    private BigDecimal precioDinamico;
    private LocalDate fechaVencimiento;
    private String estadoLote;
    private LocalDate fechaRecepcion;
}
