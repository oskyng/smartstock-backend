package com.osanzana.smartstock.auth.autenticacion.web;

import com.osanzana.smartstock.auth.autenticacion.services.RecuperacionService;
import com.osanzana.smartstock.auth.shared.dto.request.ForgotPasswordRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.ResetPasswordRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.request.VerifyCodeRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.MensajeResponseDTO;
import com.osanzana.smartstock.auth.shared.dto.response.VerifyCodeResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.BusinessException;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Recuperación de Contraseña", description = "Flujo de 'olvidé mi contraseña' mediante código verificador enviado por correo")
public class RecuperacionController {

    private static final String PROPOSITO_RESTABLECIMIENTO = "PASSWORD_RESET";
    private static final String MENSAJE_GENERICO_SOLICITUD =
            "Si el correo ingresado está registrado, recibirás un código de verificación en los próximos minutos.";

    private final RecuperacionService recuperacionService;
    private final JwtUtils jwtUtils;

    @Operation(summary = "Paso A: solicitar código de recuperación de contraseña por correo")
    @PostMapping("/forgot-password")
    public ResponseEntity<MensajeResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        recuperacionService.solicitarRecuperacion(request.getCorreo());
        // Mensaje genérico siempre, exista o no el correo: evita enumeración de usuarios.
        return ResponseEntity.ok(MensajeResponseDTO.builder().mensaje(MENSAJE_GENERICO_SOLICITUD).build());
    }

    @Operation(summary = "Paso D: verificar el código de 6 dígitos y obtener el token de restablecimiento")
    @PostMapping("/verify-code")
    public ResponseEntity<VerifyCodeResponseDTO> verifyCode(@Valid @RequestBody VerifyCodeRequestDTO request) {
        String token = recuperacionService.verificarCodigo(request.getCorreo(), request.getCodigo());
        return ResponseEntity.ok(VerifyCodeResponseDTO.builder().token(token).build());
    }

    @Operation(summary = "Paso E: restablecer la contraseña usando el token temporal de restablecimiento",
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/reset-password")
    public ResponseEntity<MensajeResponseDTO> resetPassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ResetPasswordRequestDTO request) {

        String token = extraerToken(authHeader);

        // El filtro JWT genérico ya validó firma y expiración para dejar pasar la petición
        // (requiere autenticación); aquí se exige además que sea específicamente un token de
        // restablecimiento, para que un token de login normal no pueda usarse en este endpoint.
        if (!PROPOSITO_RESTABLECIMIENTO.equals(jwtUtils.extractPurpose(token))) {
            throw new BusinessException("Token de restablecimiento inválido");
        }

        String correo = jwtUtils.extractUsername(token);
        recuperacionService.restablecerContrasena(correo, request.getNuevaContrasena());
        return ResponseEntity.ok(MensajeResponseDTO.builder().mensaje("Contraseña actualizada exitosamente.").build());
    }

    private String extraerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Token de restablecimiento no proporcionado");
        }
        return authHeader.substring(7);
    }
}
