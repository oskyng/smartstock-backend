package com.osanzana.smartstock.alert.shared.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaEscaladaEvent {
    private Long alertaId;
    private Long loteId;
    private String productoNombre;
    private Long comercioId;
    private String descripcion;
    private LocalDateTime fechaEscalamiento;
}
