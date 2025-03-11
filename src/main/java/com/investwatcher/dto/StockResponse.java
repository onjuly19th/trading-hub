package com.investwatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockResponse {

    @JsonProperty("c")
    private double currentPrice;  // 현재 가격 (c)

    @JsonProperty("d")
    private double change;        // 가격 변화 (d)

    @JsonProperty("dp")
    private double percentageChange;  // 가격 변화율 (dp)

    @JsonProperty("h")
    private double highPrice;     // 최고 가격 (h)

    @JsonProperty("l")
    private double lowPrice;      // 최저 가격 (l)

    @JsonProperty("o")
    private double openPrice;     // 시가 (o)

    @JsonProperty("pc")
    private double previousClose; // 이전 종가 (pc)

    @JsonProperty("t")
    private long timestamp;       // 타임스탬프 (t)

    public double getCurrentPrice() {
        return currentPrice;
    }
}