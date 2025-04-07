package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import com.tradinghub.domain.trading.Order;
import com.tradinghub.domain.trading.Order.OrderSide;

/**
 * 주문 체결 요청 DTO
 * 시장가/지정가 주문에 필요한 정보를 전달하기 위한 DTO 클래스
 * 주문 체결 시 포트폴리오 업데이트를 위해 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderExecutionRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00000001", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Order side is required")
    private OrderSide side;

    /**
     * 매수 주문인지 확인
     * @return 매수 주문이면 true, 매도 주문이면 false
     */
    public boolean isBuy() {
        return OrderSide.BUY.equals(side);
    }
    
    /**
     * Order 객체로부터 OrderExecutionRequest 생성
     * @param order 주문 정보
     * @return OrderExecutionRequest 객체
     */
    public static OrderExecutionRequest from(Order order) {
        BigDecimal price = order.getExecutedPrice() != null ? 
            order.getExecutedPrice() : order.getPrice();
            
        return OrderExecutionRequest.builder()
            .symbol(order.getSymbol())
            .amount(order.getAmount())
            .price(price)
            .side(order.getSide())
            .build();
    }
} 