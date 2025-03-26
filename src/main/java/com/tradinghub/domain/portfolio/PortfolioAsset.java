package com.tradinghub.domain.portfolio;

import java.math.BigDecimal;
//import java.math.RoundingMode;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "portfolio_assets")
@Getter
@Setter
public class PortfolioAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal averagePrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 미구현 코드 ===============================
    
    // 실제 총 자산 가치와 수익/손실은 프론트엔드에서 계산
    /*
    @Column(nullable = false)
    private BigDecimal currentPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal profitLoss = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal profitLossPercentage = BigDecimal.ZERO;
    */

    /*
    public void updateProfitLoss() {
        BigDecimal currentValue = amount.multiply(currentPrice);
        BigDecimal costBasis = amount.multiply(averagePrice);
        this.profitLoss = currentValue.subtract(costBasis);
        
        if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
            this.profitLossPercentage = profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
    }

    public BigDecimal getCurrentValue() {
        return amount.multiply(currentPrice);
    }
    */
} 