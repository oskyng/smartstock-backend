package com.osanzana.smartstock.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthDTO {
    private String nombre;
    private String url;
    private String estado;
    private String estadoBaseDatos;
    private Long latenciaMs;
    private String detalle;
}
