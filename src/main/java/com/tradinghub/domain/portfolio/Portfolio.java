package com.tradinghub.domain.portfolio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import com.tradinghub.domain.user.User;

@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {
    private static final int COIN_PRECISION = 20;
    private static final int COIN_SCALE = 8;
    private static final int USD_PRECISION = 20;
    private static final int USD_SCALE = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false, precision = COIN_PRECISION, scale = COIN_SCALE)
    private BigDecimal coinBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = USD_PRECISION, scale = USD_SCALE)
    private BigDecimal usdBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL)
    private List<PortfolioAsset> assets = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder
    private Portfolio(User user, String symbol, BigDecimal initialBalance) {
        this.user = user;
        this.symbol = symbol;
        this.usdBalance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
        this.availableBalance = this.usdBalance;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    /**
     * 매수 주문 처리
     * 
     * @param symbol 암호화폐 심볼
     * @param amount 매수 수량
     * @param price 매수 가격
     * @param total 매수 총액
     */
    public void processBuyOrder(String symbol, BigDecimal amount, BigDecimal price, BigDecimal total) {
        validateOrderExecution(total, true);
        
        this.usdBalance = this.usdBalance.subtract(total);
        this.coinBalance = this.coinBalance.add(amount);
        updateAvailableBalance();
        updateTimestamp();
    }

    /**
     * 매도 주문 처리
     * 
     * @param symbol 암호화폐 심볼
     * @param amount 매도 수량
     * @param price 매도 가격
     * @param total 매도 총액
     */
    public void processSellOrder(String symbol, BigDecimal amount, BigDecimal price, BigDecimal total) {
        validateOrderExecution(amount, false);
        
        this.coinBalance = this.coinBalance.subtract(amount);
        this.usdBalance = this.usdBalance.add(total);
        updateAvailableBalance();
        updateTimestamp();
    }

    /**
     * USD 잔액 업데이트
     * 
     * @param newBalance 새로운 USD 잔액
     */
    public void updateUsdBalance(BigDecimal newBalance) {
        this.usdBalance = newBalance;
        updateAvailableBalance();
        updateTimestamp();
    }

    /**
     * 사용 가능한 잔액 업데이트
     * 현재는 USD 잔액과 동일하게 설정
     */
    private void updateAvailableBalance() {
        this.availableBalance = this.usdBalance;
    }

    /**
     * 타임스탬프 업데이트
     */
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    private void validateOrderExecution(BigDecimal amount, boolean isBuy) {
        if (isBuy) {
            if (usdBalance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient USD balance");
            }
        } else {
            if (coinBalance.compareTo(amount) < 0) {
                throw new IllegalStateException("Insufficient coin balance");
            }
        }
    }

    public static Portfolio createWithBalance(User user, String symbol, BigDecimal initialBalance) {
        return Portfolio.builder()
            .user(user)
            .symbol(symbol)
            .initialBalance(initialBalance)
            .build();
    }

    /**
     * 포트폴리오에 자산을 추가합니다.
     * 자산이 새로 생성된 경우 호출됩니다.
     * 
     * @param asset 추가할 자산
     */
    public void addAsset(PortfolioAsset asset) {
        if (this.assets != null && !this.assets.contains(asset)) {
            this.assets.add(asset);
        }
        updateTimestamp();
    }

    /**
     * 포트폴리오에서 자산을 제거합니다.
     * 자산의 수량이 0이 된 경우 호출됩니다.
     * 
     * @param asset 제거할 자산
     */
    public void removeAsset(PortfolioAsset asset) {
        if (this.assets != null) {
            this.assets.remove(asset);
        }
        updateTimestamp();
    }
}