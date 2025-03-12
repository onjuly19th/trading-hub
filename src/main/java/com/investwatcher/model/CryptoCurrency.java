package com.investwatcher.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoCurrency {
    
    @Id
    private String symbol;
    
    private BigDecimal price;
    private BigDecimal volume24h;
    private BigDecimal priceChangePercent24h;
    private BigDecimal highPrice24h;
    private BigDecimal lowPrice24h;
    private LocalDateTime lastUpdated;
} 