package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.dto.order.OrderExecutionRequest;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 서비스 파사드
 * 기존 코드와의 호환성을 위해 유지되는 클래스입니다.
 * 실제 작업은 분리된 전용 서비스들에게 위임합니다.
 * 
 * @deprecated 새로운 코드에서는 PortfolioQueryService, PortfolioCommandService를 직접 사용하세요.
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {
    
    private final PortfolioQueryService portfolioQueryService;
    private final PortfolioCommandService portfolioCommandService;
    
    /**
     * 새로운 포트폴리오를 생성합니다.
     * 
     * @param user 포트폴리오 소유자
     * @param symbol 기준 통화 심볼 (예: USDT)
     * @param initialBalance 초기 잔고
     * @return 생성된 포트폴리오
     */
    public Portfolio createPortfolio(User user, String symbol, BigDecimal initialBalance) {
        return portfolioCommandService.createPortfolio(user, symbol, initialBalance);
    }
    
    /**
     * 사용자의 포트폴리오를 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 포트폴리오 정보
     */
    public Portfolio getPortfolio(Long userId) {
        return portfolioQueryService.getPortfolio(userId);
    }
    
    /**
     * 주문 실행에 따라 포트폴리오를 업데이트합니다.
     * 
     * @param userId 사용자 ID
     * @param request 주문 실행 정보
     */
    public void updatePortfolioForOrder(Long userId, OrderExecutionRequest request) {
        portfolioCommandService.updatePortfolioForOrder(userId, request);
    }
} 