package com.tradinghub.domain.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByPortfolioUserIdOrderByExecutedAtDesc(Long userId);
} 