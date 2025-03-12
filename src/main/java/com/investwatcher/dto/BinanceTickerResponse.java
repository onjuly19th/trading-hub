package com.investwatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BinanceTickerResponse {
    
    private String symbol;
    
    @JsonProperty("lastPrice")
    private BigDecimal price;
    
    @JsonProperty("volume")
    private BigDecimal volume24h;
    
    @JsonProperty("priceChangePercent")
    private BigDecimal priceChangePercent24h;
    
    @JsonProperty("highPrice")
    private BigDecimal highPrice24h;
    
    @JsonProperty("lowPrice")
    private BigDecimal lowPrice24h;
} 