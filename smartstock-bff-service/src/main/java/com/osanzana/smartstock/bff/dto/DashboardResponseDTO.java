package com.osanzana.smartstock.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponseDTO {
    private Object productos;
    private Object lotesRecientes;
    private Object alertasPendientes;
    private Object reglasActivas;
}
