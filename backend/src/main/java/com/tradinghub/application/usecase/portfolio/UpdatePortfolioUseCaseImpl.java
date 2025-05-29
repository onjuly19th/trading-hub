package com.tradinghub.application.usecase.portfolio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tradinghub.application.dto.UpdatePortfolioCommand;
import com.tradinghub.application.exception.portfolio.PortfolioNotFoundException;
import com.tradinghub.application.exception.portfolio.PortfolioUpdateException;
import com.tradinghub.application.service.portfolio.PortfolioOrderHandler;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.portfolio.PortfolioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdatePortfolioUseCaseImpl implements UpdatePortfolioUseCase {
    private final PortfolioRepository portfolioRepository;
    private final List<PortfolioOrderHandler> orderHandlers;

    @Override
    public void execute(Long userId, UpdatePortfolioCommand command) {
        Portfolio portfolio = portfolioRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new PortfolioNotFoundException("Portfolio not found for user: " + userId));

        BigDecimal orderAmount = command.amount().multiply(command.price());

        try {
            // 적절한 주문 처리 전략 찾기
            PortfolioOrderHandler handler = orderHandlers.stream()
                .filter(p -> p.supports(command))
                .findFirst()
                .orElseThrow(() -> new PortfolioUpdateException("No suitable order handler found"));
            
            // 주문 처리 실행
            handler.processOrder(portfolio, command, orderAmount);
            
        } catch (Exception e) {
            throw new PortfolioUpdateException("포트폴리오 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
