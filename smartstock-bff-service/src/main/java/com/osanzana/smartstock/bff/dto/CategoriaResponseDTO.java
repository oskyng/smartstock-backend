package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaResponseDTO {
    private Long id;
    private String nombre;
}
