package com.tradinghub.domain.order.dto;

import java.math.BigDecimal;

import com.tradinghub.domain.order.Order;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 거래 주문 요청을 위한 DTO 클래스입니다.
 * 주문에 필요한 모든 정보를 포함하며, 각 필드에 대한 유효성 검증 규칙을 정의합니다.
 */
@Getter
@Setter
public class OrderRequest {
    /**
     * 거래 대상 암호화폐의 심볼
     * 예: BTCUSDT, ETHUSDT
     */
    @NotBlank(message = "Symbol is required")
    private String symbol;

    /**
     * 주문 유형
     * MARKET: 시장가 주문
     * LIMIT: 지정가 주문
     */
    @NotNull(message = "Order type is required")
    private Order.OrderType type;

    /**
     * 주문 방향
     * BUY: 매수 주문
     * SELL: 매도 주문
     */
    @NotNull(message = "Order side is required")
    private Order.OrderSide side;

    /**
     * 주문 가격 (USD)
     * LIMIT 주문의 경우에만 필수
     * 최소값: 0.00000001 USD
     * 최대값: 1,000,000,000 USD
     */
    @DecimalMin(value = "0.00000001", message = "Price must be greater than 0")
    @DecimalMax(value = "1000000000", message = "Price is too high")
    private BigDecimal price;

    /**
     * 주문 수량
     * 매수/매도하고자 하는 암호화폐의 수량
     * 최소값: 0.00000001
     * 최대값: 1,000,000,000
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000000", message = "Amount is too high")
    private BigDecimal amount;
} 