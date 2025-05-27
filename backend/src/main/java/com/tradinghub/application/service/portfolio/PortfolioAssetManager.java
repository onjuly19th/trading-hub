package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioAsset;
import com.tradinghub.domain.model.portfolio.PortfolioAssetRepository;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 자산 관리를 담당하는 클래스
 */
@Component
@RequiredArgsConstructor
public class PortfolioAssetManager {
    
    private final PortfolioAssetRepository assetRepository;
    private final PortfolioRepository portfolioRepository;
    
    /**
     * 매수 시 자산을 업데이트합니다.
     * 기존 자산이 없는 경우 새로운 자산을 생성합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param symbol 자산 심볼
     * @param amount 매수 수량
     * @param price 매수 가격
     */
    @ExecutionTimeLog
    public void updateAssetOnBuy(Portfolio portfolio, String symbol, BigDecimal amount, BigDecimal price) {
        // 기존 자산 조회 또는 새 자산 생성
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseGet(() -> createNewAsset(portfolio, symbol));
        
        // 자산 업데이트
        updateAssetAmountAndPrice(asset, amount, price);
        
        // 저장
        assetRepository.save(asset);
    }
    
    /**
     * 매도 시 자산을 업데이트합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param asset 매도할 자산
     * @param amount 매도 수량
     */
    @ExecutionTimeLog
    public void updateAssetOnSell(Portfolio portfolio, PortfolioAsset asset, BigDecimal amount) {
        // 매도 후 자산 수량 업데이트
        asset.setAmount(asset.getAmount().subtract(amount));
        
        if (asset.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            // 자산 수량이 0이면 자산 삭제 처리
            removeAsset(portfolio, asset);
        } else {
            // 자산 업데이트 후 저장
            assetRepository.save(asset);
            portfolioRepository.save(portfolio);
        }
    }
    
    /**
     * 새로운 자산을 생성합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param symbol 자산 심볼
     * @return 생성된 자산
     */
    private PortfolioAsset createNewAsset(Portfolio portfolio, String symbol) {
        PortfolioAsset newAsset = new PortfolioAsset();
        newAsset.setPortfolio(portfolio);
        newAsset.setSymbol(symbol);
        
        // 새 자산인 경우 포트폴리오에 추가
        portfolio.addAsset(newAsset);
        
        return newAsset;
    }
    
    /**
     * 매수 시 자산의 수량과 평균가를 업데이트합니다.
     * 
     * @param asset 대상 자산
     * @param amount 매수 수량
     * @param price 매수 가격
     */
    private void updateAssetAmountAndPrice(PortfolioAsset asset, BigDecimal amount, BigDecimal price) {
        BigDecimal oldAmount = asset.getAmount() != null ? asset.getAmount() : BigDecimal.ZERO;
        BigDecimal oldAveragePrice = asset.getAveragePrice() != null ? asset.getAveragePrice() : BigDecimal.ZERO;
        BigDecimal newAmount = oldAmount.add(amount);
        
        // 평균 매수가 계산: ((기존수량 * 기존평균가) + (신규수량 * 현재가)) / 총수량
        BigDecimal totalCost = oldAmount.multiply(oldAveragePrice).add(amount.multiply(price));
        BigDecimal newAveragePrice = totalCost.divide(newAmount, 4, RoundingMode.HALF_UP);
        
        asset.setAmount(newAmount);
        asset.setAveragePrice(newAveragePrice);
    }
    
    /**
     * 포트폴리오에서 자산을 제거합니다.
     * 이 메서드는 자산의 수량이 0이 되었을 때 호출됩니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param asset 제거할 자산
     */
    @ExecutionTimeLog
    private void removeAsset(Portfolio portfolio, PortfolioAsset asset) {
        // 영속성 관리를 위해 컬렉션에서 제거
        portfolio.removeAsset(asset);
        
        // 포트폴리오 저장 후 자산 삭제
        portfolioRepository.save(portfolio);
        assetRepository.delete(asset);
    }
}
