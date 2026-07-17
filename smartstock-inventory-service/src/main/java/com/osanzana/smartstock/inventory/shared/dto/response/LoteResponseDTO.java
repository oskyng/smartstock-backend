package com.osanzana.smartstock.inventory.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoteResponseDTO {
    private Long id;
    private String nombreProducto;
    private String nombreCategoria;
    private Integer cantidadActual;
    private BigDecimal costoUnitario;
    private BigDecimal precioDinamico;
    private LocalDate fechaVencimiento;
    private String estadoLote;
    private LocalDate fechaRecepcion;
}
