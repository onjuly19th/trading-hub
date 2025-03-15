package com.tradinghub.domain.trading.dto;

import com.tradinghub.domain.trading.Order;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class OrderResponse {
    private final Long id;
    private final String symbol;
    private final String type;
    private final String side;
    private final BigDecimal price;
    private final BigDecimal amount;
    // TODO: 부분 체결 구현
    // private final BigDecimal filledAmount;
    private final String status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.symbol = order.getSymbol();
        this.type = order.getType().name();
        this.side = order.getSide().name();
        this.price = order.getPrice();
        this.amount = order.getAmount();
        // TODO: 부분 체결 구현
        // this.filledAmount = order.getFilledAmount();
        this.status = order.getStatus().name();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
    }
} 