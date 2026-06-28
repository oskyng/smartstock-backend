package com.osanzana.smartstock.finance.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoBaseDTO<T> {
    private String idEvento;
    private String tipoEvento;
    private LocalDateTime fechaEvento;
    private T payload;
}
