package com.investwatcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.investwatcher.dto.StockResponse;

@Service
public class StockService {
    
    @Value("${stock.api.key}")
    private String apiKey;

    @Value("${stock.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ExchangeRateService exchangeRateService;
    
    public StockService(RestTemplate restTemplate, ExchangeRateService exchangeRateService) {
        this.restTemplate = restTemplate;
        this.exchangeRateService = exchangeRateService;
    }

    public String getStockPrice(String symbol) {        
        String url = apiUrl + symbol + "&token=" + apiKey;
        StockResponse response = restTemplate.getForObject(url, StockResponse.class);

        if (response == null) {
            throw new RuntimeException("Invalid API response or symbol not found.");
        }

        double usdPrice = response.getCurrentPrice();
        double krwRate = exchangeRateService.getKRWRate();
        double krwPrice = usdPrice * krwRate;

        return String.format("USD: %.2f, KRW: %.2f", usdPrice, krwPrice);
    }
}