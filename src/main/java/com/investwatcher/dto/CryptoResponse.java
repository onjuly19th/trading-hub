package com.investwatcher.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public class CryptoResponse {
    @JsonProperty
    private Map<String, PriceData> data = new HashMap<>();

    @JsonAnySetter
    public void add(String key, PriceData value) {
        data.put(key, value);
    }

    public Map<String, PriceData> getData() {
        return data;
    }

    public void setData(Map<String, PriceData> data) {
        this.data = data;
    }

    public static class PriceData {
        @JsonProperty
        private double usd;

        public double getUsd() {
            return usd;
        }

        public void setUsd(double usd) {
            this.usd = usd;
        }
    }
}