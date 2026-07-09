package com.osanzana.smartstock.auth.shared.dto.request;

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
public class UsuarioCreateRequestDTO {
    @NotBlank(message = "RUT es obligatorio")
    @Size(max = 12)
    private String rut;

    @NotBlank(message = "Nombre es obligatorio")
    @Size(max = 50)
    private String nombre;

    @NotBlank(message = "Apellido es obligatorio")
    @Size(max = 50)
    private String apellido;

    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password es obligatorio")
    @Size(min = 6, message = "Password debe tener al menos 6 caracteres")
    private String password;

    private Long idComercio;
}
