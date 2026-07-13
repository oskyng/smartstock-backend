package com.osanzana.smartstock.inventory.shared.dto.request;

import jakarta.validation.constraints.Email;
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
public class ProveedorRequestDTO {

    @NotBlank(message = "El RUT de la empresa es obligatorio")
    @Size(max = 12)
    private String rutEmpresa;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 100)
    private String razonSocial;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "El email de contacto debe ser válido")
    @Size(max = 100)
    private String contactoEmail;
}
