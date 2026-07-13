package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReglaDepreciacionResponseDTO {
    private Long id;
    private String nombreCategoria;
    private Integer diasCriticosMin;
    private BigDecimal porcentajeDescuento;
    private String nombreGerente;
    private Integer activa;
}
