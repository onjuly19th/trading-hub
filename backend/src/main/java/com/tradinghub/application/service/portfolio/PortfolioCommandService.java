package com.tradinghub.application.service.portfolio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.portfolio.PortfolioNotFoundException;
import com.tradinghub.application.exception.portfolio.PortfolioUpdateException;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;
import com.tradinghub.interfaces.dto.order.OrderExecutionRequest;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 생성/수정 전용 서비스
 * 쓰기 작업만을 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class PortfolioCommandService {
    
    private final PortfolioRepository portfolioRepository;
    private final List<PortfolioOrderHandler> orderHandlers;
    
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
     * 주문 실행에 따라 포트폴리오를 업데이트합니다.
     * 
     * 처리 과정:
     * 1. 포트폴리오 조회 (비관적 락 사용)
     * 2. 적절한 주문 처리 전략 선택
     * 3. 주문 처리 실행
     * 
     * @param userId 사용자 ID
     * @param request 주문 실행 정보
     * @throws PortfolioNotFoundException 포트폴리오를 찾을 수 없는 경우
     * @throws PortfolioUpdateException 포트폴리오 업데이트 중 오류 발생 시
     */
    @ExecutionTimeLog
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void updatePortfolioForOrder(Long userId, OrderExecutionRequest request) {
        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));

        BigDecimal orderAmount = request.getAmount().multiply(request.getPrice());

        try {
            // 적절한 주문 처리 전략 찾기
            PortfolioOrderHandler handler = orderHandlers.stream()
                .filter(p -> p.supports(request))
                .findFirst()
                .orElseThrow(() -> new PortfolioUpdateException("No suitable order handler found"));
            
            // 주문 처리 실행
            handler.processOrder(portfolio, request, orderAmount);
            
        } catch (Exception e) {
            throw new PortfolioUpdateException("포트폴리오 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
