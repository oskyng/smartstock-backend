package com.osanzana.smartstock.inventory.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProveedorResponseDTO {
    private Long id;
    private String rutEmpresa;
    private String razonSocial;
    private String contactoEmail;
}
