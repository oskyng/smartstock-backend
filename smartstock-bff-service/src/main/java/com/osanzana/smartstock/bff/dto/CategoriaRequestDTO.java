package com.osanzana.smartstock.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaRequestDTO {
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 50)
    private String nombre;
}
