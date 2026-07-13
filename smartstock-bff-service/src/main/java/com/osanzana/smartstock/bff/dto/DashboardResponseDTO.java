package com.osanzana.smartstock.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDTO {
    private List<ProductoResponseDTO> productos;
    private List<LoteResponseDTO> lotesRecientes;
    private List<AlertaResponseDTO> alertasPendientes;
    private List<ReglaDepreciacionResponseDTO> reglasActivas;
}
