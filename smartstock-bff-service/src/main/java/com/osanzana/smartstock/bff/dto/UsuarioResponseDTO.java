package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String rut;
    private String nombre;
    private String apellido;
    private String email;
    private String rol;
    private Long idComercio;
    private Integer activo;
}
