package com.investwatcher.service;

import com.investwatcher.dto.ExchangeRateResponse;
import com.investwatcher.model.ExchangeRate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class ExchangeRateService {

    @Value("${exchange.api.url}")
    private String apiUrl;

    @Value("${exchange.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private ExchangeRate cachedRate;

    public ExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private void fetchExchangeRate() {
        String url = apiUrl + apiKey + "/latest/" + "USD";
        ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

        if (response != null && response.getConversionRates().containsKey("KRW")) {
            double rate = response.getConversionRates().get("KRW");
            LocalDateTime now = LocalDateTime.now();

            cachedRate = ExchangeRate.builder()
                .currency("USD")
                .rate(rate)
                .updatedAt(now)
                .build();
        }
    }

    public double getKRWRate() {
        if (cachedRate == null) {
            fetchExchangeRate();
            return getKRWRate();
        }

        long hoursSinceLastUpdate = ChronoUnit.HOURS.between(cachedRate.getUpdatedAt(), LocalDateTime.now());

        if (hoursSinceLastUpdate > 24) {
            fetchExchangeRate();
            return getKRWRate();
        }

        return cachedRate.getRate();
    }
}