package com.investwatcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
       
        
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        System.out.println("API Response: " + responseEntity.getBody());

        // API 응답을 DTO로 변환
        StockResponse response = restTemplate.getForObject(url, StockResponse.class);
        System.out.println("Parsed Response: " + response);

        // 응답 데이터가 없거나, 해당 심볼이 없으면 예외 처리
        if (response == null) {
            throw new RuntimeException("Invalid API response or symbol not found. Response: " 
                    + responseEntity.getBody() + ", Symbol: " + symbol);
        }

        double usdPrice = response.getCurrentPrice();
        double krwRate = exchangeRateService.getKRWRate();
        double krwPrice = usdPrice * krwRate;

        return String.format("USD: %.2f, KRW: %.2f", usdPrice, krwPrice);
    }
}