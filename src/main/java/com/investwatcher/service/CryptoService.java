package com.investwatcher.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.investwatcher.dto.CryptoResponse;

@Service
public class CryptoService {

    @Value("${crypto.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ExchangeRateService exchangeRateService;

    public CryptoService(RestTemplate restTemplate, ExchangeRateService exchangeRateService) {
        this.restTemplate = restTemplate;
        this.exchangeRateService = exchangeRateService;
    }
    
    public String getCryptoPrice(String symbol) {
        String url = apiUrl + symbol + "&vs_currencies=usd";
        /*System.out.println("Request URL: " + url);
        System.out.println("API Response: " + responseEntity.getBody());
        System.out.println("Parsed Response: " + response);*/
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        CryptoResponse response = restTemplate.getForObject(url, CryptoResponse.class);
        // 응답 데이터가 없거나, 해당 심볼이 없으면 예외 처리
        if (response == null || response.getData() == null) {
            throw new RuntimeException("Invalid API response or symbol not found. Response: " 
                    + responseEntity.getBody() + ", Symbol: " + symbol);
        }

        Map<String, CryptoResponse.PriceData> data = response.getData();
        CryptoResponse.PriceData priceData = data.get(symbol);
        if (priceData == null) {
            throw new RuntimeException("Price data for symbol not found: " + symbol);
        }

        double usdPrice = priceData.getUsd();
        double krwRate = exchangeRateService.getKRWRate();
        double krwPrice = usdPrice * krwRate;

        return String.format("USD: %.2f, KRW: %.2f", usdPrice, krwPrice);
    }
}