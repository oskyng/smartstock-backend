package com.osanzana.smartstock.finance.shared.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AlertaResponseDTO {
    private Long id;
    private String mensaje;
    private String rolAsignado;
    private LocalDateTime fechaLimite;
    private String estado;
    private LocalDateTime fechaCreacion;
}
