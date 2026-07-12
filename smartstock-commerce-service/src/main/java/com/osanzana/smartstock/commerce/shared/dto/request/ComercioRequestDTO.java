package com.osanzana.smartstock.commerce.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComercioRequestDTO {
    
    @NotBlank(message = "El RUT de empresa es obligatorio")
    @Pattern(regexp = "^[0-9]{1,2}\\.[0-9]{3}\\.[0-9]{3}-[0-9Kk]{1}$", message = "Formato de RUT inválido (ej: 76.123.456-7)")
    private String rutEmpresa;

    @NotBlank(message = "La razón social es obligatoria")
    private String razonSocial;

    private String rubro;
}
