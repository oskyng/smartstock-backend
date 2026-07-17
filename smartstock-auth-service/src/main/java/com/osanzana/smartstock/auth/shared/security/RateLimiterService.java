package com.osanzana.smartstock.auth.shared.security;

import com.osanzana.smartstock.auth.shared.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo de intentos en memoria, por clave (típicamente el correo del recurso atacado),
 * para login y verificación de código de recuperación. No requiere infraestructura adicional
 * (Redis, etc.) porque cada instancia del servicio mantiene su propio contador; suficiente para
 * el volumen y topología actual del despliegue (una única instancia por servicio).
 */
@Component
public class RateLimiterService {

    private static final int MAX_INTENTOS = 5;
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Intentos> registro = new ConcurrentHashMap<>();

    public void verificarNoBloqueado(String clave) {
        Intentos intentos = registro.get(clave);
        if (intentos == null) {
            return;
        }
        if (intentos.contador < MAX_INTENTOS) {
            return;
        }
        if (Duration.between(intentos.ultimoIntento, Instant.now()).compareTo(BLOQUEO) < 0) {
            throw new TooManyAttemptsException("Demasiados intentos fallidos. Intenta nuevamente en unos minutos.");
        }
        registro.remove(clave);
    }

    public void registrarFallo(String clave) {
        registro.compute(clave, (k, actual) -> {
            int contador = (actual == null) ? 1 : actual.contador + 1;
            return new Intentos(contador, Instant.now());
        });
    }

    public void registrarExito(String clave) {
        registro.remove(clave);
    }

    private record Intentos(int contador, Instant ultimoIntento) {}
}
