package com.tradinghub.domain.model.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tradinghub.domain.model.user.User;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * 거래 주문을 나타내는 엔티티 클래스
 * 
 * 시장가 주문과 지정가 주문을 모두 지원하며, 주문의 생성부터 체결/취소까지의 
 * 전체 생명주기를 관리합니다.
 * 
 * 낙관적 락({@code @Version})을 사용하여 동시성을 제어하며,
 * 상태 변경은 항상 검증 로직을 통해 이루어집니다.
 *
 * @see OrderType 주문 유형 (시장가/지정가)
 * @see OrderSide 주문 방향 (매수/매도)
 * @see OrderStatus 주문 상태 (대기/체결/취소/실패)
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    /** 주문 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문 생성 사용자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * 거래 심볼
     * 
     * 거래 쌍을 나타내는 식별자로, 다음과 같은 형식을 따릅니다:
     * - 티커(ticker): 기본 암호화폐 심볼 (예: BTC)
     * - 심볼(symbol): 거래소에서 사용하는 전체 심볼 (예: BTCUSDT)
     * - 이름(name): 사용자 친화적인 표시 이름 (예: BTC/USDT)
     * 
     * 현재 시스템에서는 심볼(symbol) 형식을 사용하며, 
     * 예를 들어 "BTCUSDT"와 같이 티커와 기준 화폐를 붙여서 사용합니다.
     */
    @Column(nullable = false)
    private String symbol;
    
    /** 주문 수량 */
    @Column(nullable = false)
    private BigDecimal amount;
    
    /** 주문 희망가 */
    @Column(nullable = false)
    private BigDecimal price;

    /** 실제 체결 가격 (시장가 주문 또는 체결된 지정가 주문) */
    @Column(name = "executed_price")
    private BigDecimal executedPrice;
    
    /** 주문 유형 (MARKET/LIMIT) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderType type;
    
    /** 매수/매도 구분 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderSide side;
    
    /** 주문 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;
    
    /** 주문 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 마지막 수정 시각 */
    private LocalDateTime updatedAt;

    /** 낙관적 락을 위한 버전 */
    @Version
    private Long version;

    /**
     * 주문 생성을 위한 빌더 메서드
     * 
     * @param user 주문 생성 사용자
     * @param symbol 거래 심볼
     * @param type 주문 유형
     * @param side 매수/매도 구분
     * @param price 주문 가격
     * @param amount 주문 수량
     * @param status 초기 상태 (기본값: PENDING)
     */
    @Builder
    private Order(User user, String symbol, OrderType type, OrderSide side,
                 BigDecimal price, BigDecimal amount, OrderStatus status) {
        this.user = user;
        this.symbol = symbol;
        this.type = type;
        this.side = side;
        this.price = price;
        this.amount = amount;
        this.status = status != null ? status : OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문을 취소 상태로 변경
     * 대기 상태의 주문만 취소 가능
     * 
     * @throws IllegalStateException 이미 체결되었거나 취소된 주문을 취소하려 할 때
     */
    public void cancel() {
        validateCanCancel();
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문을 체결 상태로 변경
     * 취소되거나 이미 체결된 주문은 체결할 수 없음
     * 
     * @throws IllegalStateException 취소되었거나 이미 체결된 주문을 체결하려 할 때
     */
    public void fill() {
        validateCanFill();
        this.status = OrderStatus.FILLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 취소 가능 여부 검증
     */
    private void validateCanCancel() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Can only cancel pending orders");
        }
    }

    /**
     * 주문 체결 가능 여부 검증
     */
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

    /**
     * 주문 유형을 나타내는 열거형
     */
    public enum OrderType {
        /** 시장가 주문: 현재 시장 가격으로 즉시 체결 */
        MARKET,
        /** 지정가 주문: 지정된 가격에 도달하면 체결 */
        LIMIT
    }

    /**
     * 주문 방향을 나타내는 열거형
     */
    public enum OrderSide {
        /** 매수 주문 */
        BUY,
        /** 매도 주문 */
        SELL
    }

    /**
     * 주문 상태를 나타내는 열거형
     */
    public enum OrderStatus {
        /** 대기: 초기 상태, 체결 대기 중 */
        PENDING,
        /** 체결: 주문이 성공적으로 체결됨 */
        FILLED,
        /** 취소: 사용자에 의해 취소됨 */
        CANCELLED,
        /** 실패: 주문 처리 중 오류 발생 */
        FAILED
    }

    /**
     * 주문이 현재 가격에 실행 가능한지 검사
     *
     * @param currentPrice 현재 시장 가격
     * @return 실행 가능 여부
     */
    public boolean isExecutableAt(BigDecimal currentPrice) {
        if (status != OrderStatus.PENDING || type != OrderType.LIMIT) {
            return false;
        }
        
        if (side == OrderSide.BUY) {
            // 매수 주문: 현재가가 주문가 이하면 실행 가능
            return currentPrice.compareTo(price) <= 0;
        } else {
            // 매도 주문: 현재가가 주문가 이상이면 실행 가능
            return currentPrice.compareTo(price) >= 0;
        }
    }

    /**
     * 주문이 특정 상태인지 확인
     *
     * @param orderStatus 확인할 주문 상태
     * @return 해당 상태인지 여부
     */
    public boolean hasStatus(OrderStatus orderStatus) {
        return this.status == orderStatus;
    }

    /**
     * 주문 총액 계산
     *
     * @return 주문 총액 (수량 * 가격)
     */
    public BigDecimal calculateTotalAmount() {
        return this.amount.multiply(this.price);
    }
} 