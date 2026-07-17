package com.osanzana.smartstock.auth.shared.security;

import com.osanzana.smartstock.auth.shared.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterServiceTest {

    @Test
    void verificarNoBloqueado_SinIntentosPrevios_NoLanza() {
        RateLimiterService rateLimiter = new RateLimiterService();
        assertDoesNotThrow(() -> rateLimiter.verificarNoBloqueado("nuevo@test.cl"));
    }

    @Test
    void verificarNoBloqueado_MenosDeCincoFallos_NoLanza() {
        RateLimiterService rateLimiter = new RateLimiterService();
        for (int i = 0; i < 4; i++) {
            rateLimiter.registrarFallo("victima@test.cl");
        }
        assertDoesNotThrow(() -> rateLimiter.verificarNoBloqueado("victima@test.cl"));
    }

    @Test
    void verificarNoBloqueado_CincoFallosConsecutivos_Bloquea() {
        RateLimiterService rateLimiter = new RateLimiterService();
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFallo("victima@test.cl");
        }
        assertThrows(TooManyAttemptsException.class, () -> rateLimiter.verificarNoBloqueado("victima@test.cl"));
    }

    @Test
    void registrarExito_ReseteaElContador() {
        RateLimiterService rateLimiter = new RateLimiterService();
        for (int i = 0; i < 4; i++) {
            rateLimiter.registrarFallo("victima@test.cl");
        }
        rateLimiter.registrarExito("victima@test.cl");
        rateLimiter.registrarFallo("victima@test.cl");

        // Solo 1 fallo tras el reset (aunque hubo 5 fallos en total antes del éxito): no debe bloquear.
        assertDoesNotThrow(() -> rateLimiter.verificarNoBloqueado("victima@test.cl"));
    }

    @Test
    void verificarNoBloqueado_ClavesDistintas_NoSeAfectanEntreSi() {
        RateLimiterService rateLimiter = new RateLimiterService();
        for (int i = 0; i < 5; i++) {
            rateLimiter.registrarFallo("atacado@test.cl");
        }
        assertDoesNotThrow(() -> rateLimiter.verificarNoBloqueado("otro@test.cl"));
    }
}
