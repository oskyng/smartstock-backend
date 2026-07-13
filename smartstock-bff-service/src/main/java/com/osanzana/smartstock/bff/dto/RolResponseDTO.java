package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RolResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
}
