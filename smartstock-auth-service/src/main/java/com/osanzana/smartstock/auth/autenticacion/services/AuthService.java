package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.dto.request.AuthRequestDTO;
import com.osanzana.smartstock.auth.shared.dto.response.AuthResponseDTO;
import com.osanzana.smartstock.auth.shared.exception.BusinessException;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthResponseDTO login(AuthRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado post-autenticación"));

        UserDetails userDetails = new User(
                usuario.getEmail(),
                usuario.getPasswordHash(),
                Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()))
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("rol", usuario.getRol().getNombre());
        if ("ADMIN_SISTEMA".equals(usuario.getRol().getNombre())) {
            claims.put("idComercio", null);
        } else if (usuario.getComercio() != null) {
            claims.put("idComercio", usuario.getComercio().getId());
        } else {
            claims.put("idComercio", null);
        }

        String token = jwtUtils.generateToken(userDetails, claims);

        return AuthResponseDTO.builder()
                .token(token)
                .email(usuario.getEmail())
                .rol(usuario.getRol().getNombre())
                .idComercio(usuario.getComercio() != null ? usuario.getComercio().getId() : null)
                .build();
    }
}
