package com.tradinghub.domain.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Getter;

import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioAsset;

@Getter
@Builder
public class PortfolioResponse {
    private BigDecimal usdBalance;
    private List<AssetResponse> assets;

    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .usdBalance(portfolio.getUsdBalance())
                .assets(portfolio.getAssets().stream()
                        .map(AssetResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class AssetResponse {
        private String symbol;
        private BigDecimal amount;
        private BigDecimal averagePrice;

        public static AssetResponse from(PortfolioAsset asset) {
            return AssetResponse.builder()
                    .symbol(asset.getSymbol())
                    .amount(asset.getAmount())
                    .averagePrice(asset.getAveragePrice())
                    .build();
        }
    }
} 