package com.osanzana.smartstock.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

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

    @Bean
    public WebClient authWebClient() {
        return WebClient.builder()
                .baseUrl(authServiceUrl)
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient commerceWebClient() {
        return WebClient.builder()
                .baseUrl(commerceServiceUrl)
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient inventoryWebClient() {
        return WebClient.builder()
                .baseUrl(inventoryServiceUrl)
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient financeWebClient() {
        return WebClient.builder()
                .baseUrl(financeServiceUrl)
                .filter(headerPropagationFilter())
                .build();
    }

    @Bean
    public WebClient alertWebClient() {
        return WebClient.builder()
                .baseUrl(alertServiceUrl)
                .filter(headerPropagationFilter())
                .build();
    }

    private ExchangeFilterFunction headerPropagationFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest servletRequest = attributes.getRequest();
                String authHeader = servletRequest.getHeader(HttpHeaders.AUTHORIZATION);
                String commerceHeader = servletRequest.getHeader("X-Comercio-ID");

                ClientRequest.Builder builder = ClientRequest.from(request);
                if (authHeader != null) {
                    builder.header(HttpHeaders.AUTHORIZATION, authHeader);
                }
                if (commerceHeader != null) {
                    builder.header("X-Comercio-ID", commerceHeader);
                }
                return Mono.just(builder.build());
            }
            return Mono.just(request);
        });
    }
}
