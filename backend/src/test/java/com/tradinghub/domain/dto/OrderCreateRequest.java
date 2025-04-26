package com.tradinghub.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderType;

/**
 * 테스트 전용 주문 생성 요청 DTO 클래스
 * 대규모 주문 테스트에서 사용
 */
@Getter
@Setter
@Builder
public class OrderCreateRequest {
    private String symbol;
    private BigDecimal amount;
    private BigDecimal price;
    private OrderSide side;
    private OrderType type;
} 