package com.tradinghub.domain.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tradinghub.domain.order.Order;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 정보를 클라이언트에 전달하기 위한 응답 DTO 클래스입니다.
 * 주문의 상세 정보와 실행 결과를 포함합니다.
 */
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    /** 주문 고유 식별자 */
    private Long id;
    
    /** 거래 대상 암호화폐 심볼 (예: BTCUSDT) */
    private String symbol;
    
    /** 주문 유형 (MARKET/LIMIT) */
    private String type;
    
    /** 주문 방향 (BUY/SELL) */
    private String side;
    
    /** 주문 가격 (USD) */
    private BigDecimal price;
    
    /** 주문 수량 */
    private BigDecimal amount;
    
    /** 주문 상태 (PENDING/COMPLETED/CANCELLED/FAILED) */
    private String status;
    
    /** 주문 생성 시각 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /** 주문 최종 수정 시각 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /** 주문 처리 결과 메시지 */
    private String message;
    
    /** 주문 실행 횟수 (부분 체결 시 사용) */
    private int executedCount;
    
    /** 실제 체결된 가격 */
    private BigDecimal executedPrice;
    
    /** 응답 생성 시간 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Order 엔티티로부터 DTO 인스턴스를 생성합니다.
     * 
     * @param order 변환할 Order 엔티티
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
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 메시지와 실행 카운트로 DTO 인스턴스를 생성합니다.
     * 주로 주문 처리 결과를 전달할 때 사용됩니다.
     * 
     * @param message 결과 메시지
     * @param executedCount 실행 횟수
     */
    private OrderResponse(String message, int executedCount) {
        this.message = message;
        this.executedCount = executedCount;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Order 엔티티를 OrderResponse DTO로 변환합니다.
     * 
     * @param order 변환할 Order 엔티티
     * @return 변환된 OrderResponse 객체
     */
    public static OrderResponse from(Order order) {
        return new OrderResponse(order);
    }
    
    /**
     * 메시지와 실행 카운트를 포함한 OrderResponse 객체를 생성합니다.
     * 
     * @param message 결과 메시지
     * @param executedCount 실행 횟수
     * @return 생성된 OrderResponse 객체
     */
    public static OrderResponse withMessage(String message, int executedCount) {
        return new OrderResponse(message, executedCount);
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