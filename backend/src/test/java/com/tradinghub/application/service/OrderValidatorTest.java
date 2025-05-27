package com.tradinghub.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tradinghub.application.exception.order.InvalidOrderException;
import com.tradinghub.application.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.application.service.order.OrderValidator;
import com.tradinghub.application.service.portfolio.PortfolioQueryService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.exception.auth.UnauthorizedOperationException;

@ExtendWith(MockitoExtension.class)
class OrderValidatorTest {

    @Mock
    private PortfolioQueryService portfolioQueryService;

    @InjectMocks
    private OrderValidator orderValidator;

    private User testUser;
    private Long userId = 1L;
    private String symbol = "BTCUSDT";
    private BigDecimal price = new BigDecimal("50000.00");
    private BigDecimal amount = new BigDecimal("0.5");
    private Order.OrderSide side = Order.OrderSide.BUY;
    private Portfolio testPortfolio;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testUser");
        testUser.setPassword("password");

        testPortfolio = Portfolio.createWithBalance(testUser, symbol, new BigDecimal("100000.00"));
    }

    @Test
    @DisplayName("매수 주문 생성 검증 성공")
    void validateOrderCreation_Success_BuyOrder() {
        // GIVEN
        given(portfolioQueryService.getPortfolio(userId)).willReturn(testPortfolio);

        // WHEN & THEN
        orderValidator.validateOrderCreation(testUser, side, price, amount);
    }

    @Test
    @DisplayName("매도 주문 생성 검증 성공")
    void validateOrderCreation_Success_SellOrder() {
        // GIVEN
        // 먼저 코인을 구매하여 잔고 확보
        testPortfolio.processBuyOrder(symbol, amount, price, price.multiply(amount));
        given(portfolioQueryService.getPortfolio(userId)).willReturn(testPortfolio);

        // WHEN & THEN
        orderValidator.validateOrderCreation(testUser, Order.OrderSide.SELL, price, amount);
    }

    @Test
    @DisplayName("주문 생성 검증 실패 - 수량이 0")
    void validateOrderCreation_Failure_ZeroAmount() {
        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCreation(testUser, side, price, BigDecimal.ZERO))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Order amount must be greater than zero");
    }

    @Test
    @DisplayName("주문 생성 검증 실패 - 수량이 음수")
    void validateOrderCreation_Failure_NegativeAmount() {
        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCreation(testUser, side, price, new BigDecimal("-0.5")))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Order amount must be greater than zero");
    }

    @Test
    @DisplayName("매수 주문 생성 검증 실패 - USD 잔고 부족")
    void validateOrderCreation_Failure_InsufficientUsdBalance() {
        // GIVEN
        testPortfolio = Portfolio.createWithBalance(testUser, symbol, new BigDecimal("1000.00")); // 필요한 금액보다 적은 잔고
        given(portfolioQueryService.getPortfolio(userId)).willReturn(testPortfolio);

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCreation(testUser, side, price, amount))
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessageContaining("Insufficient USD balance");
    }

    @Test
    @DisplayName("매도 주문 생성 검증 실패 - 코인 잔고 부족")
    void validateOrderCreation_Failure_InsufficientCoinBalance() {
        // GIVEN
        testPortfolio = Portfolio.createWithBalance(testUser, symbol, new BigDecimal("100000.00"));
        testPortfolio.processBuyOrder(symbol, new BigDecimal("0.1"), price, price.multiply(new BigDecimal("0.1"))); // 0.1 BTC 구매
        given(portfolioQueryService.getPortfolio(userId)).willReturn(testPortfolio);

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCreation(testUser, Order.OrderSide.SELL, price, amount))
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessageContaining("Insufficient coin balance");
    }

    @Test
    @DisplayName("주문 취소 검증 성공")
    void validateOrderCancellation_Success() {
        // GIVEN
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(Order.OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(Order.OrderStatus.PENDING)
                .build();

        // WHEN & THEN
        orderValidator.validateOrderCancellation(order, userId);
    }

    @Test
    @DisplayName("주문 취소 검증 실패 - 권한 없음")
    void validateOrderCancellation_Failure_Unauthorized() {
        // GIVEN
        User otherUser = new User();
        otherUser.setId(2L);
        
        Order order = Order.builder()
                .user(otherUser)
                .symbol(symbol)
                .type(Order.OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(Order.OrderStatus.PENDING)
                .build();

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCancellation(order, userId))
            .isInstanceOf(UnauthorizedOperationException.class)
            .hasMessage("order cancellation");
    }

    @Test
    @DisplayName("주문 취소 검증 실패 - 이미 체결된 주문")
    void validateOrderCancellation_Failure_AlreadyFilled() {
        // GIVEN
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(Order.OrderType.MARKET)
                .side(side)
                .price(price)
                .amount(amount)
                .status(Order.OrderStatus.FILLED)
                .build();

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCancellation(order, userId))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Cannot cancel order with status: FILLED");
    }

    @Test
    @DisplayName("주문 취소 검증 실패 - 이미 취소된 주문")
    void validateOrderCancellation_Failure_AlreadyCancelled() {
        // GIVEN
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(Order.OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(Order.OrderStatus.CANCELLED)
                .build();

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderValidator.validateOrderCancellation(order, userId))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessageContaining("Cannot cancel order with status: CANCELLED");
    }
} 