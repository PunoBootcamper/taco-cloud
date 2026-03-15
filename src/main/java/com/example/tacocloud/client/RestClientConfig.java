package com.example.tacocloud.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración para cliente REST
 */
@Configuration
public class RestClientConfig {
    
    /**
     * Bean de RestTemplate para consumir APIs REST
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
