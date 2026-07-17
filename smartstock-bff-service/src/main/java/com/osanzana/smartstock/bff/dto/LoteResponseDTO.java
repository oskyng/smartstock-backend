package com.osanzana.smartstock.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class LoteResponseDTO {
    private Long id;
    private String nombreProducto;
    private String nombreCategoria;
    private Integer cantidadActual;

    /**
     * Precio de costo: se recibe de inventory-service para calcular "Capital en Riesgo" en el
     * dashboard, pero WRITE_ONLY evita que se serialice de vuelta al frontend — ningún rol,
     * ni siquiera GERENTE_TIENDA, debe ver el costo por lote en la respuesta de la API, solo
     * el agregado ya calculado (DashboardResponseDTO.capitalEnRiesgo).
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private BigDecimal costoUnitario;

    private BigDecimal precioDinamico;
    private LocalDate fechaVencimiento;
    private String estadoLote;
    private LocalDate fechaRecepcion;
}
