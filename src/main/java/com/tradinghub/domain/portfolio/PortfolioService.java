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
import com.tradinghub.common.exception.portfolio.PortfolioUpdateException;
import com.tradinghub.domain.order.dto.OrderExecutionRequest;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 포트폴리오 관리를 담당하는 서비스 클래스
 * 
 * 주요 책임:
 * 1. 포트폴리오 생성 및 조회
 * 2. 주문 실행에 따른 포트폴리오 업데이트
 * 3. 자산 관리 (추가, 수정, 삭제)
 * 4. 잔고 및 자산 검증
 * 
 * 동시성 제어:
 * - 포트폴리오 업데이트 시 비관적 락 사용
 * - REPEATABLE_READ 격리 수준으로 데이터 일관성 보장
 * 
 * @see Portfolio 포트폴리오 엔티티
 * @see PortfolioAsset 포트폴리오 자산 엔티티
 * @see OrderExecutionRequest 주문 실행 요청
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioAssetRepository assetRepository;

    /**
     * 새로운 포트폴리오를 생성합니다.
     * 
     * @param user 포트폴리오 소유자
     * @param symbol 기준 통화 심볼 (예: USDT)
     * @param initialBalance 초기 잔고
     * @return 생성된 포트폴리오
     */
    @ExecutionTimeLog
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Portfolio createPortfolio(User user, String symbol, BigDecimal initialBalance) {
        Portfolio portfolio = Portfolio.createWithBalance(user, symbol, initialBalance);
        return portfolioRepository.save(portfolio);
    }

    /**
     * 사용자의 포트폴리오를 조회합니다.
     * 연관된 자산 정보도 함께 조회됩니다.
     * 
     * @param userId 사용자 ID
     * @return 포트폴리오 정보
     * @throws PortfolioNotFoundException 포트폴리오를 찾을 수 없는 경우
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(Long userId) {
        return portfolioRepository.findByUserIdWithAssets(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));
    }

    /**
     * 주문 실행에 따라 포트폴리오를 업데이트합니다.
     * 
     * 처리 과정:
     * 1. 포트폴리오 조회 (비관적 락 사용)
     * 2. 주문 유형에 따른 검증
     * 3. 잔고 업데이트
     * 4. 자산 업데이트
     * 
     * @param userId 사용자 ID
     * @param request 주문 실행 정보
     * @throws PortfolioNotFoundException 포트폴리오를 찾을 수 없는 경우
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     * @throws InsufficientAssetException 매도할 자산이 부족한 경우
     * @throws PortfolioUpdateException 포트폴리오 업데이트 중 오류 발생 시
     */
    @ExecutionTimeLog
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
        } catch (PortfolioNotFoundException | InsufficientBalanceException | InsufficientAssetException | AssetNotFoundException e) {
            // 이미 정의된 비즈니스 예외는 그대로 던짐
            throw e;
        } catch (Exception e) {
            // 기타 예외는 PortfolioUpdateException으로 래핑
            throw new PortfolioUpdateException("포트폴리오 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 매수 주문에 대한 포트폴리오 검증을 수행합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param orderAmount 주문 금액
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     */
    @ExecutionTimeLog
    private void validateBuyOrder(Portfolio portfolio, BigDecimal orderAmount) {
        if (portfolio.getAvailableBalance().compareTo(orderAmount) < 0) {
            throw new InsufficientBalanceException(
                String.format("Insufficient balance. Required: %s, Available: %s",
                    orderAmount, portfolio.getAvailableBalance())
            );
        }
    }

    /**
     * 매도 주문에 대한 자산 검증을 수행합니다.
     * 
     * @param portfolio 대상 포트폴리오
     * @param symbol 매도할 자산 심볼
     * @param amount 매도 수량
     * @return 검증된 자산 정보
     * @throws AssetNotFoundException 자산을 찾을 수 없는 경우
     * @throws InsufficientAssetException 매도할 자산이 부족한 경우
     */
    @ExecutionTimeLog
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
    private void updateAssetOnBuyOrder(Portfolio portfolio, String symbol, 
                                     BigDecimal amount, BigDecimal price) {
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

    /**
     * 매수 시 자산을 업데이트합니다.
     * 
     * @param asset 대상 자산
     * @param amount 매수 수량
     * @param price 매수 가격
     */
    @ExecutionTimeLog
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
     * 매수 주문에 대한 포트폴리오 처리
     * 
     * @param portfolio 대상 포트폴리오
     * @param request 매수 주문 요청
     * @param orderAmount 주문 금액
     */
    @ExecutionTimeLog
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
     * 
     * @param portfolio 대상 포트폴리오
     * @param request 매도 주문 요청
     * @param orderAmount 주문 금액
     */
    @ExecutionTimeLog
    private void processSellOrder(Portfolio portfolio, OrderExecutionRequest request, BigDecimal orderAmount) {
        PortfolioAsset asset = validateSellOrder(portfolio, request.getSymbol(), request.getAmount());
        
        // processSellOrder는 내부적으로 usdBalance를 업데이트함
        portfolio.processSellOrder(request.getSymbol(), request.getAmount(), request.getPrice(), orderAmount);
        
        // 매도 후 자산 업데이트
        asset.setAmount(asset.getAmount().subtract(request.getAmount()));
        
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