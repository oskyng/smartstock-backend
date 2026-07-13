package com.osanzana.smartstock.bff.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertaAuditoriaResponseDTO {
    private Long id;
    private Long loteId;
    private String productoNombre;
    private String codigoBarra;
    private Long usuarioAsignadoId;
    private String usuarioAsignadoNombre;
    private String estadoAlerta;
    private LocalDateTime fechaLimiteAtencion;
    private LocalDateTime fechaAtencion;
    private String descripcionAlerta;
}
