package com.tradinghub.domain.portfolio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;
import java.math.BigDecimal;
//import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tradinghub.domain.user.User;
import com.tradinghub.domain.trading.Trade;

@Entity
@Table(name = "portfolios")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {
    private static final int COIN_PRECISION = 20;
    private static final int COIN_SCALE = 8;
    private static final int USD_PRECISION = 20;
    private static final int USD_SCALE = 2;
    //private static final int PERCENTAGE_SCALE = 4;
    //private static final BigDecimal HUNDRED = new BigDecimal("100");

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

    // 웹소켓 비활성화로 인해 프론트엔드에서 계산하지만, DB 스키마 변경 방지를 위해 필드 유지
    /* 
    @Column(nullable = false)
    private BigDecimal totalBalance = BigDecimal.ZERO;
    */

    @Column(nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /*
    // 웹소켓 비활성화로 인해 프론트엔드에서 계산하지만, DB 스키마 변경 방지를 위해 필드 유지
    @Column(nullable = false)
    private BigDecimal totalProfitLoss = BigDecimal.ZERO;

    // 웹소켓 비활성화로 인해 프론트엔드에서 계산하지만, DB 스키마 변경 방지를 위해 필드 유지
    @Column(nullable = false)
    private BigDecimal profitLossPercentage = BigDecimal.ZERO;
    */

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL)
    private List<PortfolioAsset> assets = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL)
    private List<Trade> trades = new ArrayList<>();

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
        // this.totalBalance = this.usdBalance;
        // 웹소켓 비활성화로 인해 수정 - 단순히 USD 잔액으로 설정
        // this.totalBalance = this.usdBalance;
        this.availableBalance = this.usdBalance;
        // this.totalProfitLoss = BigDecimal.ZERO;
        // this.profitLossPercentage = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
        this.usdBalance = availableBalance;
        //updateTotalBalance();
        // 웹소켓 비활성화로 인해 수정 - 단순히 USD 잔액으로 설정
        // this.totalBalance = this.usdBalance;
        this.updatedAt = LocalDateTime.now();
    }

    // 웹소켓 비활성화
    /*
    private void updateTotalBalance() {
        this.totalBalance = this.usdBalance;
        updateTotalBalance();
        BigDecimal assetValue = assets.stream()
            .map(asset -> asset.getAmount().multiply(asset.getCurrentPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(USD_SCALE, RoundingMode.HALF_UP);
        
        this.totalBalance = this.usdBalance.add(assetValue);
        updateProfitLoss();
    }
    */

    /*
    private void updateProfitLoss() {
        if (totalBalance.compareTo(BigDecimal.ZERO) > 0) {
            this.profitLossPercentage = totalProfitLoss
                .divide(totalBalance, PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
        }
        // 웹소켓 비활성화로 인해 자산 가치 계산 로직 제거
        // 단순히 USD 잔액만 설정
        // this.totalBalance = this.usdBalance;
        // 수익/손실 값을 0으로 설정
        // this.totalProfitLoss = BigDecimal.ZERO;
        // this.profitLossPercentage = BigDecimal.ZERO;
        this.updatedAt = LocalDateTime.now();
    }
    */

    public void processBuyTrade(Trade trade) {
        validateTradeExecution(trade.getTotal(), true);
        
        this.usdBalance = this.usdBalance.subtract(trade.getTotal());
        this.coinBalance = this.coinBalance.add(trade.getAmount());
        this.availableBalance = this.usdBalance;
        //updateTotalBalance(trade.getPrice());
        
        // 웹소켓 비활성화로 인해 수정 - 단순히 USD 잔액으로 설정
        // 실제 총 자산 가치와 수익/손실은 프론트엔드에서 계산
        // this.totalBalance = this.usdBalance;
        // this.totalProfitLoss = BigDecimal.ZERO;
        // this.profitLossPercentage = BigDecimal.ZERO;
        
        trades.add(trade);
        //updateProfitLoss();
        this.updatedAt = LocalDateTime.now();
    }

    public void processSellTrade(Trade trade) {
        validateTradeExecution(trade.getAmount(), false);
        
        this.coinBalance = this.coinBalance.subtract(trade.getAmount());
        this.usdBalance = this.usdBalance.add(trade.getTotal());
        this.availableBalance = this.usdBalance;
        //updateTotalBalance(trade.getPrice());
        
        // 웹소켓 비활성화로 인해 수정 - 단순히 USD 잔액으로 설정
        // 실제 총 자산 가치와 수익/손실은 프론트엔드에서 계산
        // this.totalBalance = this.usdBalance;
        // this.totalProfitLoss = BigDecimal.ZERO;
        // this.profitLossPercentage = BigDecimal.ZERO;
        
        trades.add(trade);
        //updateProfitLoss();
    }

    private void validateTradeExecution(BigDecimal amount, boolean isBuy) {
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

    // 웹소켓 비활성화로 인해 수정 - 단순히 USD 잔액으로 설정
    /* 
    private void updateTotalBalance(BigDecimal currentPrice) {
        this.totalBalance = this.usdBalance.add(
            this.coinBalance.multiply(currentPrice)
                .setScale(USD_SCALE, RoundingMode.HALF_UP)
        );
    }
    */

    public void addAsset(PortfolioAsset asset) {
        assets.add(asset);
        asset.setPortfolio(this);
    }

    public static Portfolio createWithBalance(User user, String symbol, BigDecimal initialBalance) {
        return Portfolio.builder()
            .user(user)
            .symbol(symbol)
            .initialBalance(initialBalance)
            .build();
    }
} 