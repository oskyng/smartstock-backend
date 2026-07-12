package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ComercioResponseDTO {
    private Long id;
    private String rutEmpresa;
    private String razonSocial;
    private String rubro;
    private LocalDate fechaRegistro;
    private String estado;
}
