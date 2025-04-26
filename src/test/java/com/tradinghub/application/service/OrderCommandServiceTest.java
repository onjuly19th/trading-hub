package com.tradinghub.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tradinghub.application.service.order.OrderCommandService;
import com.tradinghub.application.service.order.OrderValidator;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderStatus;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.OrderRepository;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.interfaces.exception.order.InvalidOrderException;
import com.tradinghub.interfaces.exception.order.OrderExecutionException;
import com.tradinghub.interfaces.exception.order.OrderNotFoundException;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderValidator orderValidator;

    @InjectMocks
    private OrderCommandService orderCommandService;

    private User testUser;
    private Long userId = 1L;
    private Long orderId = 100L;
    private String symbol = "BTCUSDT";
    private BigDecimal price = new BigDecimal("50000.00");
    private BigDecimal amount = new BigDecimal("0.5");
    private OrderSide side = OrderSide.BUY;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setUsername("testUser");
        testUser.setPassword("password");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("시장가 주문 생성 성공")
    void createMarketOrder_Success() {
        // GIVEN
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });

        // WHEN
        Order order = orderCommandService.createMarketOrder(userId, symbol, side, price, amount);

        // THEN
        then(userRepository).should(times(1)).findById(userId);
        then(orderValidator).should(times(1)).validateOrderCreation(testUser, side, price, amount);
        then(orderRepository).should(times(1)).save(any(Order.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getUser()).isEqualTo(testUser);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getSide()).isEqualTo(side);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getExecutedPrice()).isEqualTo(price);
    }

    @Test
    @DisplayName("지정가 주문 생성 성공")
    void createLimitOrder_Success() {
        // GIVEN
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", orderId);
            return order;
        });

        // WHEN
        Order order = orderCommandService.createLimitOrder(userId, symbol, side, price, amount);

        // THEN
        then(userRepository).should(times(1)).findById(userId);
        then(orderValidator).should(times(1)).validateOrderCreation(testUser, side, price, amount);
        then(orderRepository).should(times(1)).save(any(Order.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getUser()).isEqualTo(testUser);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getSide()).isEqualTo(side);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("주문 생성 실패 - 사용자 없음")
    void createOrder_Failure_UserNotFound() {
        // GIVEN
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("User not found");

        then(userRepository).should(times(1)).findById(userId);
        then(orderValidator).shouldHaveNoInteractions();
        then(orderRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("주문 생성 실패 - 유효성 검증 실패")
    void createOrder_Failure_ValidationFailed() {
        // GIVEN
        given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
        doThrow(new InvalidOrderException("Invalid order parameters"))
            .when(orderValidator).validateOrderCreation(testUser, side, price, amount);

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Invalid order parameters");

        then(userRepository).should(times(1)).findById(userId);
        then(orderValidator).should(times(1)).validateOrderCreation(testUser, side, price, amount);
        then(orderRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("주문 실행 성공")
    void executeOrder_Success() {
        // GIVEN
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // WHEN
        Order executedOrder = orderCommandService.executeOrder(order);

        // THEN
        then(orderRepository).should(times(1)).save(order);
        assertThat(executedOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() {
        // GIVEN
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderRepository.save(any(Order.class))).willReturn(order);

        // WHEN
        Order cancelledOrder = orderCommandService.cancelOrder(orderId, userId);

        // THEN
        then(orderRepository).should(times(1)).findById(orderId);
        then(orderValidator).should(times(1)).validateOrderCancellation(order, userId);
        then(orderRepository).should(times(1)).save(order);
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("주문 취소 실패 - 주문 없음")
    void cancelOrder_Failure_OrderNotFound() {
        // GIVEN
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderCommandService.cancelOrder(orderId, userId))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found with id: " + orderId);

        then(orderRepository).should(times(1)).findById(orderId);
        then(orderValidator).shouldHaveNoInteractions();
        then(orderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("주문 취소 실패 - 유효성 검증 실패")
    void cancelOrder_Failure_ValidationFailed() {
        // GIVEN
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        
        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        doThrow(new InvalidOrderException("Cannot cancel an already filled order"))
            .when(orderValidator).validateOrderCancellation(order, userId);

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderCommandService.cancelOrder(orderId, userId))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Cannot cancel an already filled order");

        then(orderRepository).should(times(1)).findById(orderId);
        then(orderValidator).should(times(1)).validateOrderCancellation(order, userId);
        then(orderRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("가격에 따른 주문 실행 성공")
    void executeOrdersAtPrice_Success() {
        // GIVEN
        BigDecimal currentPrice = new BigDecimal("49000.00"); // 매수 주문 체결 가능한 가격
        
        Order buyOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(buyOrder, "id", orderId);
        
        Order sellOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.SELL)
                .price(new BigDecimal("51000.00"))
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(sellOrder, "id", orderId + 1);
        
        List<Order> executableOrders = Arrays.asList(buyOrder, sellOrder);
        
        given(orderRepository.findExecutableOrders(symbol, currentPrice)).willReturn(executableOrders);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        int executedCount = orderCommandService.executeOrdersAtPrice(symbol, currentPrice);

        // THEN
        then(orderRepository).should(times(1)).findExecutableOrders(symbol, currentPrice);
        then(orderRepository).should(times(2)).save(any(Order.class));
        assertThat(executedCount).isEqualTo(2);
        assertThat(buyOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(sellOrder.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    @DisplayName("가격에 따른 주문 실행 - 실행 가능한 주문 없음")
    void executeOrdersAtPrice_NoExecutableOrders() {
        // GIVEN
        BigDecimal currentPrice = new BigDecimal("49000.00");
        given(orderRepository.findExecutableOrders(symbol, currentPrice)).willReturn(List.of());

        // WHEN
        int executedCount = orderCommandService.executeOrdersAtPrice(symbol, currentPrice);

        // THEN
        then(orderRepository).should(times(1)).findExecutableOrders(symbol, currentPrice);
        then(orderRepository).shouldHaveNoMoreInteractions();
        assertThat(executedCount).isEqualTo(0);
    }

    @Test
    @DisplayName("가격에 따른 주문 실행 실패 - 주문 실행 중 오류")
    void executeOrdersAtPrice_Failure_ExecutionError() {
        // GIVEN
        BigDecimal currentPrice = new BigDecimal("49000.00");
        
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        
        List<Order> executableOrders = List.of(order);
        
        given(orderRepository.findExecutableOrders(symbol, currentPrice)).willReturn(executableOrders);
        doThrow(new RuntimeException("Database error"))
            .when(orderRepository).save(any(Order.class));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderCommandService.executeOrdersAtPrice(symbol, currentPrice))
            .isInstanceOf(OrderExecutionException.class)
            .hasMessageContaining("주문 체결 처리 중 오류 발생");

        then(orderRepository).should(times(1)).findExecutableOrders(symbol, currentPrice);
        then(orderRepository).should(times(1)).save(any(Order.class));
    }
} 