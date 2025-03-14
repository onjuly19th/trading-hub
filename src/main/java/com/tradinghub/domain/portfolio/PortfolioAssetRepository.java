package com.tradinghub.domain.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    Optional<PortfolioAsset> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
} 