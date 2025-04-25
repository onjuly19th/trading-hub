package com.tradinghub.domain.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

/**
 * 포트폴리오 내의 개별 자산을 나타내는 엔티티 클래스
 * 특정 암호화폐의 보유 수량과 평균 매수가를 관리합니다.
 */
@Entity
@Table(name = "portfolio_assets")
@Getter
@Setter
public class PortfolioAsset {
    /** 자산 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 자산이 속한 포트폴리오 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    /** 암호화폐 심볼 (예: BTC, ETH) */
    @Column(nullable = false)
    private String symbol;

    /** 보유 수량 */
    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    /** 평균 매수가 (USD) */
    @Column(nullable = false)
    private BigDecimal averagePrice = BigDecimal.ZERO;

    /** 자산 생성 시각 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** 자산 최종 수정 시각 */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** 낙관적 락을 위한 버전 */
    @Version
    private Long version;

    /**
     * 엔티티 생성 시 자동으로 호출되어 생성 시각과 수정 시각을 설정합니다.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 엔티티 수정 시 자동으로 호출되어 수정 시각을 갱신합니다.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 