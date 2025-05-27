package com.tradinghub.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tradinghub.application.service.order.OrderApplicationService;
import com.tradinghub.application.service.order.OrderExecutionService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderStatus;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.user.User;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderApplicationService orderApplicationService;

    @InjectMocks
    private OrderExecutionService orderExecutionService;

    private User testUser;
    private String symbol = "BTCUSDT";
    private BigDecimal currentPrice = new BigDecimal("50000.00");

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setPassword("password");
    }

    @Test
    @DisplayName("체결 가능한 주문이 없을 때는 아무 작업도 수행하지 않음")
    void checkAndExecuteOrders_NoExecutableOrders() {
        // GIVEN
        given(orderRepository.findExecutableOrders(symbol, currentPrice))
            .willReturn(Collections.emptyList());

        // WHEN
        orderExecutionService.checkAndExecuteOrders(symbol, currentPrice);

        // THEN
        then(orderApplicationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("체결 가능한 주문이 있을 때 모든 주문을 체결 처리")
    void checkAndExecuteOrders_WithExecutableOrders() {
        // GIVEN
        Order buyOrder = Order.builder()
            .user(testUser)
            .symbol(symbol)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .price(new BigDecimal("49000.00"))
            .amount(new BigDecimal("0.1"))
            .status(OrderStatus.PENDING)
            .build();

        Order sellOrder = Order.builder()
            .user(testUser)
            .symbol(symbol)
            .type(OrderType.LIMIT)
            .side(OrderSide.SELL)
            .price(new BigDecimal("51000.00"))
            .amount(new BigDecimal("0.1"))
            .status(OrderStatus.PENDING)
            .build();

        List<Order> executableOrders = Arrays.asList(buyOrder, sellOrder);
        given(orderRepository.findExecutableOrders(symbol, currentPrice))
            .willReturn(executableOrders);

        // WHEN
        orderExecutionService.checkAndExecuteOrders(symbol, currentPrice);

        // THEN
        then(orderApplicationService).should(times(2)).executeOrder(any(Order.class));
        then(orderApplicationService).should(times(1)).executeOrder(buyOrder);
        then(orderApplicationService).should(times(1)).executeOrder(sellOrder);
    }

    @Test
    @DisplayName("체결 가능한 주문이 있을 때 주문을 체결 처리")
    void checkAndExecuteOrders_WithExecutableOrders_ExecuteOrder() {
        // GIVEN
        Order buyOrder = Order.builder()
            .user(testUser)
            .symbol(symbol)
            .type(OrderType.LIMIT)
            .side(OrderSide.BUY)
            .price(new BigDecimal("49000.00"))
            .amount(new BigDecimal("0.1"))
            .status(OrderStatus.PENDING)
            .build();

        List<Order> executableOrders = Collections.singletonList(buyOrder);
        given(orderRepository.findExecutableOrders(symbol, currentPrice))
            .willReturn(executableOrders);

        // WHEN
        orderExecutionService.checkAndExecuteOrders(symbol, currentPrice);

        // THEN
        then(orderApplicationService).should(times(1)).executeOrder(buyOrder);
    }
} 