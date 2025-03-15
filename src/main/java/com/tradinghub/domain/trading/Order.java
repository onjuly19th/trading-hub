package com.tradinghub.domain.trading;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.tradinghub.domain.user.User;
import lombok.Getter;
import lombok.AccessLevel;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String symbol;  // 티커
    
    @Column(nullable = false)
    private BigDecimal amount;  // 주문 수량
    
    @Column(nullable = false)
    private BigDecimal price;  // 주문 가격

    @Column(name = "executed_price")
    private BigDecimal executedPrice;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;  // MARKET, LIMIT
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;  // BUY, SELL
    
    // TODO: 부분 체결 구현
    // private BigDecimal filledAmount;  // 체결된 수량
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder
    private Order(User user, String symbol, OrderType type, OrderSide side,
                 BigDecimal price, BigDecimal amount, OrderStatus status) {
        this.user = user;
        this.symbol = symbol;
        this.type = type;
        this.side = side;
        this.price = price;
        this.amount = amount;
        // TODO: 부분 체결 구현
        // this.filledAmount = BigDecimal.ZERO;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        validateCanCancel();
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    // TODO: 부분 체결 구현
    public void fill() {
        validateCanFill();
        // this.filledAmount = this.amount;
        this.status = OrderStatus.FILLED;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateCanCancel() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending orders");
        }
    }

    // TODO: 부분 체결 구현
    private void validateCanFill() {
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot fill cancelled order");
        }
        if (status == OrderStatus.FILLED) {
            throw new IllegalStateException("Order is already filled");
        }
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void setExecutedPrice(BigDecimal executedPrice) {
        this.executedPrice = executedPrice;
        this.updatedAt = LocalDateTime.now();
    }

    public enum OrderType {
        MARKET,   // 시장가 주문
        LIMIT     // 지정가 주문
    }

    public enum OrderSide {
        BUY,      // 매수
        SELL      // 매도
    }

    public enum OrderStatus {
        PENDING,           // 대기
        // TODO: 부분 체결 구현
        // PARTIALLY_FILLED,  // 일부 체결
        FILLED,           // 완전 체결
        CANCELLED         // 취소됨
    }
} 