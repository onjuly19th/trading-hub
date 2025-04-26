package com.tradinghub.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.Order.OrderSide;

import lombok.Getter;

/**
 * 주문 체결 이벤트
 * 주문이 체결되었을 때 발생하는 이벤트
 */
@Getter
public class OrderExecutedEvent implements DomainEvent {
    private final Long orderId;
    private final Long userId;
    private final String symbol;
    private final OrderSide side;
    private final BigDecimal amount;
    private final BigDecimal price;
    private final LocalDateTime executedAt;
    
    /**
     * Order 객체로부터 이벤트 생성
     */
    public OrderExecutedEvent(Order order) {
        this.orderId = order.getId();
        this.userId = order.getUser().getId();
        this.symbol = order.getSymbol();
        this.side = order.getSide();
        this.amount = order.getAmount();
        this.price = order.getExecutedPrice() != null ? order.getExecutedPrice() : order.getPrice();
        this.executedAt = LocalDateTime.now();
    }
} 