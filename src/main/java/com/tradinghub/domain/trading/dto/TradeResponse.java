package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

import com.tradinghub.domain.trading.Trade;
import com.tradinghub.domain.trading.Trade.TradeType;

@Getter
@Builder
public class TradeResponse {
    private Long id;
    private String symbol;
    private TradeType type;
    private BigDecimal amount;
    private BigDecimal price;
    private BigDecimal total;
    private LocalDateTime executedAt;

    public static TradeResponse from(Trade trade) {
        return TradeResponse.builder()
                .id(trade.getId())
                .symbol(trade.getSymbol())
                .type(trade.getType())
                .amount(trade.getAmount())
                .price(trade.getPrice())
                .total(trade.getTotal())
                .executedAt(trade.getExecutedAt())
                .build();
    }
} 