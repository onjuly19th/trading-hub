package com.investwatcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CryptoService {

    @Value("${crypto.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public CryptoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getCryptoPrice(String symbol) {
        String url = apiUrl + symbol + "&vs_currencies=usd";
        return restTemplate.getForObject(url, String.class);
    }
}