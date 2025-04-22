package com.tradinghub.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.common.exception.portfolio.AssetNotFoundException;
import com.tradinghub.common.exception.portfolio.InsufficientAssetException;
import com.tradinghub.common.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.common.exception.portfolio.PortfolioNotFoundException;
import com.tradinghub.domain.trading.dto.OrderExecutionRequest;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.aop.LogExecutionTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository assetRepository;

    @LogExecutionTime
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio createPortfolio(User user, String symbol, BigDecimal initialBalance) {
        Portfolio portfolio = Portfolio.createWithBalance(user, symbol, initialBalance);
        return portfolioRepository.save(portfolio);
    }

    @LogExecutionTime
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        return portfolioRepository.findByUserIdWithAssets(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));
    }

    @LogExecutionTime
    private void validateBuyOrder(Portfolio portfolio, BigDecimal orderAmount) {
        if (portfolio.getAvailableBalance().compareTo(orderAmount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %s, Available: %s",
                    orderAmount, portfolio.getAvailableBalance())
            );
        }
    }

    @LogExecutionTime
    private PortfolioAsset validateSellOrder(Portfolio portfolio, String symbol, BigDecimal amount) {
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseThrow(() -> new AssetNotFoundException("Asset not found: " + symbol));
            
        if (asset.getAmount().compareTo(amount) < 0) {
            throw new InsufficientAssetException(
                String.format("Insufficient asset amount. Required: %s, Available: %s",
                    amount, asset.getAmount())
            );
        }
        return asset;
    }

    @LogExecutionTime
    private void updateAssetOnBuyOrder(Portfolio portfolio, String symbol, BigDecimal amount, BigDecimal price) {
        // 기존 자산 조회 또는 새 자산 생성
        PortfolioAsset asset = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), symbol)
            .orElseGet(() -> {
                PortfolioAsset newAsset = new PortfolioAsset();
                newAsset.setPortfolio(portfolio);
                newAsset.setSymbol(symbol);
                
                // 새 자산인 경우 포트폴리오에 추가
                portfolio.addAsset(newAsset);
                
                return newAsset;
            });
        
        // 자산 업데이트
        updateAssetOnBuy(asset, amount, price);
        
        // 저장
        assetRepository.save(asset);
    }

    @LogExecutionTime
    private void updateAssetOnBuy(PortfolioAsset asset, BigDecimal amount, BigDecimal price) {
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
     * 주문 정보로 포트폴리오 업데이트
     * 주문 체결 시 포트폴리오 잔액과 자산을 업데이트함
     * 
     * @param userId 사용자 ID
     * @param request 주문 실행 요청 정보
     */
    @LogExecutionTime
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updatePortfolioForOrder(Long userId, OrderExecutionRequest request) {
        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));

        BigDecimal orderAmount = request.getAmount().multiply(request.getPrice());

        try {
            if (request.isBuy()) {
                processBuyOrder(portfolio, request, orderAmount);
            } else {
                processSellOrder(portfolio, request, orderAmount);
            }
        } catch (Exception e) {
            log.error("Error updating portfolio: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 매수 주문에 대한 포트폴리오 처리
     */
    @LogExecutionTime
    private void processBuyOrder(Portfolio portfolio, OrderExecutionRequest request, BigDecimal orderAmount) {
        validateBuyOrder(portfolio, orderAmount);
        
        // processBuyOrder는 내부적으로 usdBalance를 업데이트함
        portfolio.processBuyOrder(request.getSymbol(), request.getAmount(), request.getPrice(), orderAmount);
        
        // 자산 업데이트
        updateAssetOnBuyOrder(portfolio, request.getSymbol(), request.getAmount(), request.getPrice());
        
        // 포트폴리오 저장
        portfolioRepository.save(portfolio);
    }

    /**
     * 매도 주문에 대한 포트폴리오 처리
     */
    @LogExecutionTime
    private void processSellOrder(Portfolio portfolio, OrderExecutionRequest request, BigDecimal orderAmount) {
        PortfolioAsset asset = validateSellOrder(portfolio, request.getSymbol(), request.getAmount());
        
        // processSellOrder는 내부적으로 usdBalance를 업데이트함
        portfolio.processSellOrder(request.getSymbol(), request.getAmount(), request.getPrice(), orderAmount);
        
        // 매도 후 자산 업데이트
        asset.setAmount(asset.getAmount().subtract(request.getAmount()));
        
        if (asset.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            // 자산 수량이 0이면 자산 삭제 처리
            removeAsset(portfolio, asset);
            log.info("Asset deleted for portfolio: portfolioId={}, symbol={}", 
                     portfolio.getId(), asset.getSymbol());
        } else {
            // 자산 업데이트 후 저장
            assetRepository.save(asset);
            portfolioRepository.save(portfolio);
        }
    }

    /**
     * 포트폴리오에서 자산을 제거합니다.
     * 이 메서드는 자산의 수량이 0이 되었을 때 호출됩니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param asset 제거할 자산
     */
    @LogExecutionTime
    private void removeAsset(Portfolio portfolio, PortfolioAsset asset) {
        // 영속성 관리를 위해 컬렉션에서 제거
        portfolio.removeAsset(asset);
        
        // 포트폴리오 저장 후 자산 삭제
        portfolioRepository.save(portfolio);
        assetRepository.delete(asset);
    }
} 