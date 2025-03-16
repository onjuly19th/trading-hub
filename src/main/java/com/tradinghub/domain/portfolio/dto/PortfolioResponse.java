package com.tradinghub.domain.portfolio.dto;

import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioAsset;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PortfolioResponse {
    private BigDecimal usdBalance;
    // 웹소켓 비활성화로 인해 프론트엔드에서 계산하므로 응답에서 제외
    // private BigDecimal totalBalance;
    private List<AssetResponse> assets;

    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .usdBalance(portfolio.getUsdBalance())
                // 웹소켓 비활성화로 인해 프론트엔드에서 계산하므로 응답에서 제외
                // .totalBalance(portfolio.getTotalBalance())
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
        // 웹소켓 비활성화로 인해 프론트엔드에서 계산하므로 응답에서 제외
        // private BigDecimal currentPrice;
        private BigDecimal averagePrice;
        // 웹소켓 비활성화로 인해 프론트엔드에서 계산하므로 응답에서 제외
        // private BigDecimal profitLoss;
        // private BigDecimal profitLossPercentage;

        public static AssetResponse from(PortfolioAsset asset) {
            return AssetResponse.builder()
                    .symbol(asset.getSymbol())
                    .amount(asset.getAmount())
                    // 웹소켓 비활성화로 인해 프론트엔드에서 계산하므로 응답에서 제외
                    // .currentPrice(asset.getCurrentPrice())
                    .averagePrice(asset.getAveragePrice())
                    // 웹소켓 비활성화로 인해 프론트엔드에서 계산하므로 응답에서 제외
                    // .profitLoss(asset.getProfitLoss())
                    // .profitLossPercentage(asset.getProfitLossPercentage())
                    .build();
        }
    }
} 