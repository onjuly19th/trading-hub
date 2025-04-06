package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.tradinghub.domain.trading.Order;

@Getter
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private String symbol;
    private String type;
    private String side;
    private BigDecimal price;
    private BigDecimal amount;
    // TODO: 부분 체결 구현
    // private final BigDecimal filledAmount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
    private int executedCount;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.symbol = order.getSymbol();
        this.type = order.getType().toString();
        this.side = order.getSide().toString();
        this.price = order.getPrice();
        this.amount = order.getAmount();
        // TODO: 부분 체결 구현
        // this.filledAmount = order.getFilledAmount();
        this.status = order.getStatus().toString();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }

    public OrderResponse(String message, int executedCount) {
        this.message = message;
        this.executedCount = executedCount;
    }
} 