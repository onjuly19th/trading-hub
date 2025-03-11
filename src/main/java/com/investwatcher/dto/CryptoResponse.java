package com.investwatcher.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class CryptoResponse {
    private String id;
    private String symbol;
    private String name;
    private String image;

    @JsonProperty("current_price")
    private BigDecimal currentPrice;
}