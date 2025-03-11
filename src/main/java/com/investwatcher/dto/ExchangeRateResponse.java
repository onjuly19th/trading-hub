package com.investwatcher.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExchangeRateResponse {

    @JsonProperty("result")
    private String result;

    @JsonProperty("base_code")
    private String base_code;

    @JsonProperty("conversion_rates")
    private Map<String, Double> conversion_rates;

    public String getResult() {
        return result;
    }

    public String getBaseCode() {
        return base_code;
    }

    public Map<String, Double> getConversionRates() {
        return conversion_rates;
    }
}