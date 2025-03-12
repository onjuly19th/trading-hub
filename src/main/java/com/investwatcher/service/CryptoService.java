package com.investwatcher.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.investwatcher.dto.CryptoResponse;
import com.investwatcher.dto.BinanceTickerResponse;
import com.investwatcher.model.CryptoCurrency;
import com.investwatcher.repository.CryptoCurrencyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CryptoService {

    @Value("${crypto.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ExchangeRateService exchangeRateService;
    private final WebClient binanceWebClient;
    private final CryptoCurrencyRepository repository;

    public CryptoService(RestTemplate restTemplate, ExchangeRateService exchangeRateService, WebClient binanceWebClient, CryptoCurrencyRepository repository) {
        this.restTemplate = restTemplate;
        this.exchangeRateService = exchangeRateService;
        this.binanceWebClient = binanceWebClient;
        this.repository = repository;
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

    public Mono<CryptoCurrency> fetchAndSavePrice(String symbol) {
        return binanceWebClient.get()
            .uri("/api/v3/ticker/24hr?symbol=" + symbol)
            .retrieve()
            .bodyToMono(BinanceTickerResponse.class)
            .map(response -> {
                CryptoCurrency crypto = CryptoCurrency.builder()
                    .symbol(response.getSymbol())
                    .price(response.getPrice())
                    .volume24h(response.getVolume24h())
                    .priceChangePercent24h(response.getPriceChangePercent24h())
                    .highPrice24h(response.getHighPrice24h())
                    .lowPrice24h(response.getLowPrice24h())
                    .lastUpdated(LocalDateTime.now())
                    .build();
                
                return repository.save(crypto);
            })
            .doOnError(error -> log.error("Failed to fetch price for " + symbol, error));
    }
}