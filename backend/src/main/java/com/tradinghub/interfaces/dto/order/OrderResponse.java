package com.tradinghub.interfaces.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tradinghub.domain.model.order.Order;

/**
 * 주문 정보를 클라이언트에 전달하기 위한 응답 DTO 레코드입니다.
 * 주문의 상세 정보와 실행 결과를 포함합니다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
    /** 주문 고유 식별자 */
    Long id,
    
    /** 거래 대상 암호화폐 심볼 (예: BTCUSDT) */
    String symbol,
    
    /** 주문 유형 (MARKET/LIMIT) */
    String type,
    
    /** 주문 방향 (BUY/SELL) */
    String side,
    
    /** 주문 가격 (USD) */
    BigDecimal price,
    
    /** 주문 수량 */
    BigDecimal amount,
    
    /** 주문 상태 (PENDING/COMPLETED/CANCELLED/FAILED) */
    String status,
    
    /** 주문 생성 시각 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt,
    
    /** 실제 체결된 가격 */
    BigDecimal executedPrice
) {
    /**
     * Order 엔티티로부터 OrderResponse 인스턴스를 생성합니다.
     * 
     * @param order 변환할 Order 엔티티
     * @return 변환된 OrderResponse 객체
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getSymbol(),
            order.getType().toString(),
            order.getSide().toString(),
            order.getPrice(),
            order.getAmount(),
            order.getStatus().toString(),
            order.getCreatedAt(),
            order.getExecutedPrice()
        );
    }
    
    /**
     * Admin API에서 메시지를 담은 응답을 생성합니다.
     * 
     * @param executedCount 실행 횟수
     * @return 생성된 OrderResponse 객체
     */
    public static OrderResponse withExecutionResult(int executedCount) {
        return new OrderResponse(
            null, 
            null, 
            null, 
            null, 
            null, 
            null, 
            "EXECUTED", 
            LocalDateTime.now(), 
            null
        );
    }
    
    /**
     * 주문 목록을 DTO 목록으로 변환합니다.
     * 
     * @param orders 변환할 Order 엔티티 목록
     * @return 변환된 OrderResponse 객체 목록
     */
    public static List<OrderResponse> fromList(List<Order> orders) {
        return orders.stream()
            .map(OrderResponse::from)
            .collect(Collectors.toList());
    }
} 