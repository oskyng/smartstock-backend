package com.osanzana.smartstock.auth.autenticacion.services;

import com.osanzana.smartstock.auth.core.entities.CodigoRecuperacion;
import com.osanzana.smartstock.auth.core.entities.Usuario;
import com.osanzana.smartstock.auth.core.repositories.CodigoRecuperacionRepository;
import com.osanzana.smartstock.auth.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.auth.shared.exception.BusinessException;
import com.osanzana.smartstock.auth.shared.security.JwtUtils;
import com.osanzana.smartstock.auth.shared.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecuperacionService {

    private static final int MINUTOS_EXPIRACION = 10;
    private static final String NO_UTILIZADO = "N";
    private static final String UTILIZADO = "S";

    private final UsuarioRepository usuarioRepository;
    private final CodigoRecuperacionRepository codigoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Paso A + B + C. No lanza excepción ni revela si el correo existe: el controller siempre
     * responde el mismo mensaje genérico, para no exponer qué correos están registrados.
     */
    @Transactional
    public void solicitarRecuperacion(String correo) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(correo);
        if (usuarioOpt.isEmpty()) {
            log.info("Solicitud de recuperación para un correo no registrado");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        invalidarCodigosActivos(usuario.getId());

        String codigo = generarCodigo();
        CodigoRecuperacion codigoRecuperacion = CodigoRecuperacion.builder()
                .usuario(usuario)
                .codigoVerificador(codigo)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(MINUTOS_EXPIRACION))
                .utilizado(NO_UTILIZADO)
                .build();
        codigoRepository.save(codigoRecuperacion);

        emailService.enviarCodigoRecuperacion(usuario.getEmail(), usuario.getNombre(), codigo);
    }

    /** Paso D: valida el código, lo invalida (uso único) y emite el token de restablecimiento. */
    @Transactional
    public String verificarCodigo(String correo, String codigo) {
        rateLimiterService.verificarNoBloqueado(correo);

        try {
            Usuario usuario = usuarioRepository.findByEmail(correo)
                    .orElseThrow(() -> new BusinessException("Código inválido o expirado"));

            CodigoRecuperacion codigoRecuperacion = codigoRepository
                    .findByUsuarioIdAndCodigoVerificadorAndUtilizado(usuario.getId(), codigo, NO_UTILIZADO)
                    .orElseThrow(() -> new BusinessException("Código inválido o expirado"));

            if (codigoRecuperacion.getFechaExpiracion().isBefore(LocalDateTime.now())) {
                throw new BusinessException("Código inválido o expirado");
            }

            codigoRecuperacion.setUtilizado(UTILIZADO);
            codigoRepository.save(codigoRecuperacion);

            rateLimiterService.registrarExito(correo);
            return jwtUtils.generarTokenRestablecimiento(usuario.getEmail());
        } catch (BusinessException ex) {
            rateLimiterService.registrarFallo(correo);
            throw ex;
        }
    }

    /** Paso E: actualiza la contraseña cifrada con BCrypt. El correo ya viene validado desde el token. */
    @Transactional
    public void restablecerContrasena(String correo, String nuevaContrasena) {
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        usuario.setPasswordHash(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);
        log.info("Contraseña restablecida exitosamente para el usuario {}", usuario.getId());
    }

    /** Código numérico de 6 dígitos (000000-999999) generado con SecureRandom. */
    private String generarCodigo() {
        int numero = secureRandom.nextInt(1_000_000);
        return String.format("%06d", numero);
    }

    private void invalidarCodigosActivos(Long usuarioId) {
        codigoRepository.findByUsuarioIdAndUtilizado(usuarioId, NO_UTILIZADO)
                .forEach(codigo -> {
                    codigo.setUtilizado(UTILIZADO);
                    codigoRepository.save(codigo);
                });
    }
}
