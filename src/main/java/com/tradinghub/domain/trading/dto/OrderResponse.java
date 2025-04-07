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
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
    private int executedCount;
    private BigDecimal executedPrice; // 체결 가격 추가

    public OrderResponse(Order order) {
        this.id = order.getId();
        this.symbol = order.getSymbol();
        this.type = order.getType().toString();
        this.side = order.getSide().toString();
        this.price = order.getPrice();
        this.amount = order.getAmount();
        this.status = order.getStatus().toString();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.executedPrice = order.getExecutedPrice();
    }

    public OrderResponse(String message, int executedCount) {
        this.message = message;
        this.executedCount = executedCount;
    }
    
    /**
     * Order 객체로부터 OrderResponse 객체 생성
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(order);
    }
} 