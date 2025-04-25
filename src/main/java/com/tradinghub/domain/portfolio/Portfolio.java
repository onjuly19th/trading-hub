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

/**
 * 사용자의 포트폴리오를 나타내는 엔티티 클래스
 * 암호화폐 거래와 관련된 잔액 정보와 자산 목록을 관리합니다.
 */
@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {
    /** 코인 수량의 전체 자릿수 */
    private static final int COIN_PRECISION = 20;
    /** 코인 수량의 소수점 자릿수 */
    private static final int COIN_SCALE = 8;
    /** USD 금액의 전체 자릿수 */
    private static final int USD_PRECISION = 20;
    /** USD 금액의 소수점 자릿수 */
    private static final int USD_SCALE = 2;

    /** 포트폴리오 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 포트폴리오 소유자 */
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** 기준 통화 심볼 (예: BTC, ETH 등) */
    @Column(nullable = false)
    private String symbol;

    /** 보유 중인 코인 수량 */
    @Column(nullable = false, precision = COIN_PRECISION, scale = COIN_SCALE)
    private BigDecimal coinBalance = BigDecimal.ZERO;

    /** USD 잔액 */
    @Column(nullable = false, precision = USD_PRECISION, scale = USD_SCALE)
    private BigDecimal usdBalance = BigDecimal.ZERO;

    /** 사용 가능한 잔액 (거래에 사용할 수 있는 금액) */
    @Column(nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /** 포트폴리오에 포함된 자산 목록 */
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL)
    private List<PortfolioAsset> assets = new ArrayList<>();

    /** 포트폴리오 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 포트폴리오 최종 수정 시각 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 낙관적 락을 위한 버전 */
    @Version
    private Long version;

    /**
     * 포트폴리오를 생성합니다.
     * 
     * @param user 포트폴리오 소유자
     * @param symbol 기준 통화 심볼
     * @param initialBalance 초기 USD 잔액
     */
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
     * 매수 주문을 처리합니다.
     * USD 잔액을 차감하고 코인 수량을 증가시킵니다.
     * 
     * @param symbol 암호화폐 심볼
     * @param amount 매수할 코인 수량
     * @param price 매수 단가
     * @param total 매수 총액 (수수료 포함)
     * @throws IllegalStateException USD 잔액이 부족한 경우
     */
    public void processBuyOrder(String symbol, BigDecimal amount, BigDecimal price, BigDecimal total) {
        validateOrderExecution(total, true);
        
        this.usdBalance = this.usdBalance.subtract(total);
        this.coinBalance = this.coinBalance.add(amount);
        updateAvailableBalance();
        updateTimestamp();
    }

    /**
     * 매도 주문을 처리합니다.
     * 코인 수량을 차감하고 USD 잔액을 증가시킵니다.
     * 
     * @param symbol 암호화폐 심볼
     * @param amount 매도할 코인 수량
     * @param price 매도 단가
     * @param total 매도 총액 (수수료 차감 전)
     * @throws IllegalStateException 코인 잔액이 부족한 경우
     */
    public void processSellOrder(String symbol, BigDecimal amount, BigDecimal price, BigDecimal total) {
        validateOrderExecution(amount, false);
        
        this.coinBalance = this.coinBalance.subtract(amount);
        this.usdBalance = this.usdBalance.add(total);
        updateAvailableBalance();
        updateTimestamp();
    }

    /**
     * USD 잔액을 업데이트합니다.
     * 
     * @param newBalance 새로운 USD 잔액
     */
    public void updateUsdBalance(BigDecimal newBalance) {
        this.usdBalance = newBalance;
        updateAvailableBalance();
        updateTimestamp();
    }

    /**
     * 사용 가능한 잔액을 업데이트합니다.
     * 현재는 USD 잔액과 동일하게 설정됩니다.
     */
    private void updateAvailableBalance() {
        this.availableBalance = this.usdBalance;
    }

    /**
     * 최종 수정 시각을 현재 시각으로 업데이트합니다.
     */
    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주문 실행 가능 여부를 검증합니다.
     * 
     * @param amount 검증할 금액 (매수 시 USD, 매도 시 코인 수량)
     * @param isBuy true인 경우 매수 주문, false인 경우 매도 주문
     * @throws IllegalStateException 잔액이 부족한 경우
     */
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

    /**
     * 초기 잔액을 가진 새로운 포트폴리오를 생성합니다.
     * 
     * @param user 포트폴리오 소유자
     * @param symbol 기준 통화 심볼
     * @param initialBalance 초기 USD 잔액
     * @return 생성된 포트폴리오
     */
    public static Portfolio createWithBalance(User user, String symbol, BigDecimal initialBalance) {
        return Portfolio.builder()
            .user(user)
            .symbol(symbol)
            .initialBalance(initialBalance)
            .build();
    }

    /**
     * 포트폴리오에 자산을 추가합니다.
     * 이미 존재하는 자산은 추가되지 않습니다.
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