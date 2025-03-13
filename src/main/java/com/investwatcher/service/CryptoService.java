package com.investwatcher.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.investwatcher.dto.BinanceTickerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class CryptoService {

    @Value("${crypto.api.url}")
    private String apiUrl;

    private final ExchangeRateService exchangeRateService;
    private final WebClient binanceWebClient;

    public CryptoService(ExchangeRateService exchangeRateService, WebClient binanceWebClient) {
        this.exchangeRateService = exchangeRateService;
        this.binanceWebClient = binanceWebClient;
    }
    
    public String getCryptoPrice(String symbol) {
        // Binance API를 통한 실시간 가격 조회
        return binanceWebClient.get()
            .uri("/api/v3/ticker/24hr?symbol=" + symbol + "USDT")
            .retrieve()
            .bodyToMono(BinanceTickerResponse.class)
            .map(response -> {
                double krwRate = exchangeRateService.getKRWRate();
                double usdPrice = response.getPrice().doubleValue();
                double krwPrice = usdPrice * krwRate;
                
                return String.format("USD: %.2f, KRW: %.2f, CHANGE: %.2f%%", 
                    usdPrice, 
                    krwPrice,
                    response.getPriceChangePercent24h().doubleValue());
            })
            .block();
    }
}