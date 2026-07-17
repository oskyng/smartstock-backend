package com.osanzana.smartstock.auth.autenticacion.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Async solo funciona en métodos public invocados desde OTRO bean (el proxy de Spring AOP se
 * salta las llamadas internas/self-invocation) — por eso el envío de correo vive en un servicio
 * separado de RecuperacionService en vez de ser un método privado ahí mismo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final int MINUTOS_EXPIRACION = 10;

    private final JavaMailSender mailSender;

    @Async
    public void enviarCodigoRecuperacion(String destinatario, String nombre, String codigo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("SmartStock - Código de recuperación de contraseña");
            mensaje.setText(String.format(
                    "Hola %s,%n%n" +
                            "Tu código de verificación para restablecer tu contraseña es: %s%n%n" +
                            "Este código expira en %d minutos. Si no solicitaste este cambio, puedes ignorar este mensaje.",
                    nombre, codigo, MINUTOS_EXPIRACION));
            mailSender.send(mensaje);
            log.info("Correo de recuperación enviado a {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar correo de recuperación a {}: {}", destinatario, e.getMessage());
        }
    }
}
