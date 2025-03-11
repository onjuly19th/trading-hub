package com.investwatcher.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
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
        String url = apiUrl + "?ids=" + symbol + "&vs_currency=" + "usd";
        /*System.out.println("Request URL: " + url);
        System.out.println("API Response: " + responseEntity.getBody());
        System.out.println("Parsed Response: " + response);*/
        List<CryptoResponse> responseList = restTemplate.exchange(
            url, 
            HttpMethod.GET,
            null, 
            new ParameterizedTypeReference<List<CryptoResponse>>() {}
        ).getBody();
        CryptoResponse response = responseList.get(0);
        // 응답 데이터가 없거나, 해당 심볼이 없으면 예외 처리

        BigDecimal usdPrice = response.getCurrentPrice();
        if (response == null || usdPrice == null) {
            throw new RuntimeException("Invalid API response or price not found.");
        }

        double krwRate = exchangeRateService.getKRWRate();
        double krwPrice = usdPrice.doubleValue() * krwRate;

        return String.format("USD: %.2f, KRW: %.2f, IMAGE: %s", usdPrice, krwPrice, response.getImage());
    }
}