package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.NoArgsConstructor;

import com.tradinghub.domain.trading.Order;

@Getter
@NoArgsConstructor // JSON 직렬화를 위해 필요
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
    private BigDecimal executedPrice;

    /**
     * Order 엔티티로부터 DTO 인스턴스 생성 (내부용)
     */
    private OrderResponse(Order order) {
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

    /**
     * 메시지와 실행 카운트로 인스턴스 생성 (내부용)
     */
    private OrderResponse(String message, int executedCount) {
        this.message = message;
        this.executedCount = executedCount;
    }
    
    /**
     * Order 객체로부터 OrderResponse 객체 생성
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(order);
    }
    
    /**
     * 메시지와 실행 카운트로부터 OrderResponse 객체 생성
     */
    public static OrderResponse withMessage(String message, int executedCount) {
        return new OrderResponse(message, executedCount);
    }
    
    /**
     * 주문 목록을 DTO 목록으로 변환
     */
    public static List<OrderResponse> fromList(List<Order> orders) {
        return orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }
} 