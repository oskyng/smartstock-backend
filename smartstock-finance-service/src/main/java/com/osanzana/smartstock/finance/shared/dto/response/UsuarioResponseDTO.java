package com.osanzana.smartstock.finance.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
