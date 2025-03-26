package com.tradinghub.domain.trading;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.tradinghub.domain.portfolio.Portfolio;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal amount;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(nullable = false, precision = 20, scale = 8)
    private BigDecimal total;

    @Column(nullable = false)
    private LocalDateTime executedAt;

    @PrePersist
    protected void onCreate() {
        executedAt = LocalDateTime.now();
    }

    public void calculateTotal() {
        this.total = this.amount.multiply(this.price);
    }

    public enum TradeType {
        BUY, SELL
    }
} 