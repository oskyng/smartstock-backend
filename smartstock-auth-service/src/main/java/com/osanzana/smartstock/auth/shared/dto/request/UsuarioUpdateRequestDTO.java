package com.osanzana.smartstock.auth.shared.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioUpdateRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 50)
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    @Size(max = 100)
    private String email;

    @NotNull(message = "El ID de rol es obligatorio")
    private Long idRol;
}
