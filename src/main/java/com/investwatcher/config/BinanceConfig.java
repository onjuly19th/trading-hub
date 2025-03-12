package com.investwatcher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BinanceConfig {
    
    private static final String BINANCE_API_BASE_URL = "https://api.binance.com";
    
    @Bean
    public WebClient binanceWebClient() {
        return WebClient.builder()
            .baseUrl(BINANCE_API_BASE_URL)
            .build();
    }
} 