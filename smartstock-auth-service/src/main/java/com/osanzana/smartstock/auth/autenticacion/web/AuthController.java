package com.osanzana.smartstock.auth.autenticacion.web;

import com.osanzana.smartstock.auth.autenticacion.services.AuthService;
import com.osanzana.smartstock.auth.shared.dto.request.AuthRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.AuthResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints para login y gestión de tokens JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login de usuario", description = "Autentica un usuario y devuelve un token JWT")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
