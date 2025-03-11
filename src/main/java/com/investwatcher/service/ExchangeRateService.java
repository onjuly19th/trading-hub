package com.investwatcher.service;

import com.investwatcher.dto.ExchangeRateResponse;
import com.investwatcher.model.ExchangeRate;
import com.investwatcher.repository.ExchangeRateRepository;
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
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateService(RestTemplate restTemplate, ExchangeRateRepository exchangeRateRepository) {
        this.restTemplate = restTemplate;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    // 환율 정보를 API로부터 받아오고 DB에 저장
    public void fetchAndSaveExchangeRate() {
        String url = apiUrl + apiKey + "/latest/" + "USD";
        ExchangeRateResponse response = restTemplate.getForObject(url, ExchangeRateResponse.class);

        if (response != null && response.getConversionRates().containsKey("KRW")) {
            double rate = response.getConversionRates().get("KRW");
            LocalDateTime now = LocalDateTime.now();

            // DB에 새로운 환율 정보 저장
            ExchangeRate exchangeRate = ExchangeRate.builder()
                .currency("USD")
                .rate(rate)
                .updatedAt(now)
                .build();
            exchangeRateRepository.save(exchangeRate);
        }
    }

    // DB에서 환율 정보 확인 후, 필요 시 업데이트
    public double getKRWRate() {
        ExchangeRate exchangeRate = exchangeRateRepository.findTopByOrderByUpdatedAtDesc();
        
        // 최신 환율이 없으면 환율을 가져옴
        if (exchangeRate == null) {
            //System.out.println("HELLO!!!!");
            fetchAndSaveExchangeRate(); // 처음 실행 시 환율 정보 없으면 새로 받아옴
            return getKRWRate(); // 재귀 호출하여 새로 받은 환율 반환
        }

        // 최신 환율이 하루 이상 경과한 경우, 환율 업데이트
        long hoursSinceLastUpdate = ChronoUnit.HOURS.between(exchangeRate.getUpdatedAt(), LocalDateTime.now());

        if (hoursSinceLastUpdate > 24) {
            fetchAndSaveExchangeRate(); // 하루가 지나면 환율 갱신
            return getKRWRate(); // 재귀 호출하여 새로 받은 환율 반환
        }

        return exchangeRate.getRate();
    }
}