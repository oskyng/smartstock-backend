package com.osanzana.smartstock.auth.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
}
