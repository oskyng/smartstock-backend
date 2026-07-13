package com.osanzana.smartstock.commerce.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComercioResponseDTO {
    private Long id;
    private String rutEmpresa;
    private String razonSocial;
    private String rubro;
    private LocalDate fechaRegistro;
    private String estado;
}
