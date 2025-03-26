package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

import com.tradinghub.domain.trading.Order;

@Getter
@Setter
public class OrderRequest {
    private String symbol;          // 티커
    private Order.OrderType type;   // 주문 유형 (MARKET, LIMIT)
    private Order.OrderSide side;   // 주문 방향 (BUY, SELL)
    private BigDecimal price;       // 주문 가격
    private BigDecimal amount;      // 주문 수량
} 