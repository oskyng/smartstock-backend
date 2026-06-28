package com.osanzana.smartstock.finance.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaResponseDTO {
    private Long id;
    private String nombre;
}
