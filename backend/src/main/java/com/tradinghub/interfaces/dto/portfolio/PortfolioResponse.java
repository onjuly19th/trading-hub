package com.tradinghub.interfaces.dto.portfolio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioAsset;

import lombok.Builder;
import lombok.Getter;

/**
 * 포트폴리오 정보를 클라이언트에 전달하기 위한 응답 DTO입니다.
 * USD 잔액과 보유 자산 목록을 포함합니다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioResponse {
    /** USD 잔액 */
    private BigDecimal usdBalance;
    
    /** 보유 자산 목록 */
    private List<AssetResponse> assets;
    
    /**
     * 응답 생성 시간
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 응답 메시지
     */
    private String message;

    /**
     * Portfolio 엔티티를 PortfolioResponse DTO로 변환합니다.
     * 
     * @param portfolio 변환할 Portfolio 엔티티
     * @return 변환된 PortfolioResponse 객체
     */
    public static PortfolioResponse from(Portfolio portfolio) {
        return PortfolioResponse.builder()
                .usdBalance(portfolio.getUsdBalance())
                .assets(portfolio.getAssets().stream()
                        .map(AssetResponse::from)
                        .collect(Collectors.toList()))
                .timestamp(LocalDateTime.now())
                .message("Portfolio retrieved successfully")
                .build();
    }

    /**
     * 포트폴리오 내 개별 자산 정보를 담는 내부 DTO 클래스입니다.
     * 암호화폐 심볼, 보유 수량, 평균 매수가 정보를 포함합니다.
     */
    @Getter
    @Builder
    public static class AssetResponse {
        /** 암호화폐 심볼 (예: BTC, ETH) */
        private String symbol;
        
        /** 보유 수량 */
        private BigDecimal amount;
        
        /** 평균 매수가 (USD) */
        private BigDecimal averagePrice;

        /**
         * PortfolioAsset 엔티티를 AssetResponse DTO로 변환합니다.
         * 
         * @param asset 변환할 PortfolioAsset 엔티티
         * @return 변환된 AssetResponse 객체
         */
        public static AssetResponse from(PortfolioAsset asset) {
            return AssetResponse.builder()
                    .symbol(asset.getSymbol())
                    .amount(asset.getAmount())
                    .averagePrice(asset.getAveragePrice())
                    .build();
        }
    }
} 