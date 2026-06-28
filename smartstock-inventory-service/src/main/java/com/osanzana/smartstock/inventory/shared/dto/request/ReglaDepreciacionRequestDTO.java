package com.osanzana.smartstock.inventory.shared.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReglaDepreciacionRequestDTO {

    @NotNull(message = "El ID de la categoría es obligatorio")
    private Long idCategoria;

    @NotNull(message = "Los días críticos son obligatorios")
    @Positive(message = "Los días críticos deben ser positivos")
    private Integer diasCriticosMin;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @Positive(message = "El descuento debe ser positivo")
    private BigDecimal porcentajeDescuento;
}
