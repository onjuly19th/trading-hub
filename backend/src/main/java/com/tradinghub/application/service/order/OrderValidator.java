package com.tradinghub.application.service.order;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.tradinghub.application.exception.order.InvalidOrderException;
import com.tradinghub.application.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.application.service.portfolio.PortfolioQueryService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.exception.auth.UnauthorizedOperationException;

import lombok.RequiredArgsConstructor;

/**
 * 주문 관련 유효성 검증을 담당하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class OrderValidator {
    private final PortfolioQueryService portfolioQueryService;
    
    /**
     * 주문 생성 시 유효성 검증
     *
     * @param user   주문 생성 사용자
     * @param side   매수/매도 구분
     * @param price  주문 가격
     * @param amount 주문 수량
     * @throws InvalidOrderException        주문이 유효하지 않은 경우
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     */
    public void validateOrderCreation(User user, Order.OrderSide side, BigDecimal price, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Order amount must be greater than zero");
        }

        Portfolio portfolio = portfolioQueryService.getPortfolio(user.getId());

        if (side == Order.OrderSide.BUY) {
            validateBuyOrder(portfolio, price, amount);
        } else {
            validateSellOrder(portfolio, amount);
        }
    }
    
    /**
     * 매수 주문 유효성 검증
     *
     * @param portfolio 사용자 포트폴리오
     * @param price     주문 가격
     * @param amount    주문 수량
     * @throws InsufficientBalanceException USD 잔고가 부족한 경우
     */
    private void validateBuyOrder(Portfolio portfolio, BigDecimal price, BigDecimal amount) {
        BigDecimal requiredUsd = price.multiply(amount);
        if (portfolio.getUsdBalance().compareTo(requiredUsd) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient USD balance. Required: %s, Available: %s",
                            requiredUsd, portfolio.getUsdBalance())
            );
        }
    }
    
    /**
     * 매도 주문 유효성 검증
     *
     * @param portfolio 사용자 포트폴리오
     * @param amount    주문 수량
     * @throws InsufficientBalanceException 코인 잔고가 부족한 경우
     */
    private void validateSellOrder(Portfolio portfolio, BigDecimal amount) {
        if (portfolio.getCoinBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient coin balance. Required: %s, Available: %s",
                            amount, portfolio.getCoinBalance())
            );
        }
    }
    
    /**
     * 주문 취소 유효성 검증
     *
     * @param order  취소할 주문
     * @param userId 요청자 ID
     * @throws UnauthorizedOperationException 권한이 없는 경우
     * @throws InvalidOrderException          이미 체결되었거나 취소된 주문인 경우
     */
    public void validateOrderCancellation(Order order, Long userId) {
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("order cancellation");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new InvalidOrderException("Cannot cancel order with status: " + order.getStatus());
        }
    }
} 