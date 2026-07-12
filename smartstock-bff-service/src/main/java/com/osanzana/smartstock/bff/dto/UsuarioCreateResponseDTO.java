package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioCreateResponseDTO {
    private String mensaje;
    private UsuarioResponseDTO usuario;
    private Long timestamp;
}
