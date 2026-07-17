package com.osanzana.smartstock.bff.config;

import com.osanzana.smartstock.bff.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${smartstock.auth-service.url}")
    private String authServiceUrl;

    @Value("${smartstock.inventory-service.url}")
    private String inventoryServiceUrl;

    @Value("${smartstock.finance-service.url}")
    private String financeServiceUrl;

    @Value("${smartstock.alert-service.url}")
    private String alertServiceUrl;

    @Value("${smartstock.commerce-service.url}")
    private String commerceServiceUrl;

    /**
     * Timeout compartido para todas las llamadas a microservicios internos: sin esto, un servicio
     * colgado agota el pool de conexiones reactivas del bff en cascada (el resto de las peticiones
     * queda esperando indefinidamente).
     */
    private static ReactorClientHttpConnector boundedConnector() {
        HttpClient httpClient = HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(5));
        return new ReactorClientHttpConnector(httpClient);
    }

    @Bean
    public WebClient authWebClient() {
        return WebClient.builder()
                .baseUrl(authServiceUrl)
                .clientConnector(boundedConnector())
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient commerceWebClient() {
        return WebClient.builder()
                .baseUrl(commerceServiceUrl)
                .clientConnector(boundedConnector())
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient inventoryWebClient() {
        return WebClient.builder()
                .baseUrl(inventoryServiceUrl)
                .clientConnector(boundedConnector())
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient financeWebClient() {
        return WebClient.builder()
                .baseUrl(financeServiceUrl)
                .clientConnector(boundedConnector())
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient alertWebClient() {
        return WebClient.builder()
                .baseUrl(alertServiceUrl)
                .clientConnector(boundedConnector())
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient healthWebClient() {
        return WebClient.builder()
                .clientConnector(boundedConnector())
                .build();
    }

    /**
     * Propaga Authorization/X-Comercio-ID del request original entrante, pero solo si la llamada
     * no trae ya un valor explícito propio (ej. el token de servicio interno para llamadas
     * bff -> microservicio que no deben viajar con el JWT del usuario real). El JWT del usuario
     * llega al bff por cookie httpOnly (no por header, ver JwtAuthenticationFilter), así que aquí
     * se reconstruye el header Authorization: Bearer a partir de esa cookie para que los
     * microservicios internos —que solo entienden Authorization— no necesiten cambiar nada.
     */
    private ExchangeFilterFunction headerPropagationFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest servletRequest = attributes.getRequest();
                String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader == null) {
                    authHeader = bearerFromCookie(servletRequest);
                }
                String commerceHeader = servletRequest.getHeader("X-Comercio-ID");

                ClientRequest.Builder builder = ClientRequest.from(request);
                if (authHeader != null && !request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                    builder.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
                if (commerceHeader != null && !request.headers().containsKey("X-Comercio-ID")) {
                    builder.header("X-Comercio-ID", commerceHeader);
                }
                return Mono.just(builder.build());
            }
            return Mono.just(request);
        });
    }

    private String bearerFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (JwtUtils.AUTH_COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return "Bearer " + cookie.getValue();
            }
        }
        return null;
    }
}
