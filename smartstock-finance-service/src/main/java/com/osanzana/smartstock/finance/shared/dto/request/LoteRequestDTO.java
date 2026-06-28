package com.osanzana.smartstock.finance.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequestDTO {
    
    @NotNull(message = "El ID del producto es obligatorio")
    private Long idProducto;

    @NotNull(message = "El ID del proveedor es obligatorio")
    private Long idProveedor;

    @NotNull(message = "La cantidad inicial es obligatoria")
    @Positive(message = "La cantidad inicial debe ser positiva")
    private Integer cantidadInicial;

    @NotNull(message = "El costo unitario es obligatorio")
    @Positive(message = "El costo debe ser positivo")
    private BigDecimal costoUnitario;

    @NotNull(message = "El precio dinámico inicial es obligatorio")
    @Positive(message = "El precio debe ser positivo")
    private BigDecimal precioDinamico;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    private LocalDate fechaVencimiento;
}
