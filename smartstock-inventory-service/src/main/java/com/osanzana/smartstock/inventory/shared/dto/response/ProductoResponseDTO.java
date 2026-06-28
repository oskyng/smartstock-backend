package com.osanzana.smartstock.inventory.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductoResponseDTO {
    private Long id;
    private String codigoBarra;
    private String nombre;
    private String nombreCategoria;
    private BigDecimal precioBase;
}
