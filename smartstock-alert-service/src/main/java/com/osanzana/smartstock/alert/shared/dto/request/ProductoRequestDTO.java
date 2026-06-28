package com.osanzana.smartstock.alert.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequestDTO {

    @NotBlank(message = "El código de barra es obligatorio")
    @Size(max = 50)
    private String codigoBarra;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotNull(message = "El ID de la categoría es obligatorio")
    private Long idCategoria;

    @NotNull(message = "El precio base es obligatorio")
    @Positive(message = "El precio base debe ser positivo")
    private BigDecimal precioBase;
}
