package com.osanzana.smartstock.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Bean
    public WebClient authWebClient() {
        return WebClient.builder().baseUrl(authServiceUrl).build();
    }

    @Bean
    public WebClient inventoryWebClient() {
        return WebClient.builder().baseUrl(inventoryServiceUrl).build();
    }

    @Bean
    public WebClient financeWebClient() {
        return WebClient.builder().baseUrl(financeServiceUrl).build();
    }

    @Bean
    public WebClient alertWebClient() {
        return WebClient.builder().baseUrl(alertServiceUrl).build();
    }
}
