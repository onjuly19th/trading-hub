package com.tradinghub.domain.portfolio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioAssetRepository extends JpaRepository<PortfolioAsset, Long> {
    Optional<PortfolioAsset> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);
} 