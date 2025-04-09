package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import lombok.Getter;
import lombok.Setter;

import com.tradinghub.domain.trading.Order;

@Getter
@Setter
public class OrderRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;          // 티커
    
    @NotNull(message = "Order type is required")
    private Order.OrderType type;   // 주문 유형 (MARKET, LIMIT)
    
    @NotNull(message = "Order side is required")
    private Order.OrderSide side;   // 주문 방향 (BUY, SELL)
    
    // price는 LIMIT 주문에만 필수이므로 컨트롤러에서 검증
    @DecimalMin(value = "0.00000001", message = "Price must be greater than 0")
    @DecimalMax(value = "1000000000", message = "Price is too high")
    private BigDecimal price;       // 주문 가격
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000000", message = "Amount is too high")
    private BigDecimal amount;      // 주문 수량
} 