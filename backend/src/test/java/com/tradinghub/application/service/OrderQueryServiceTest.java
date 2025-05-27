package com.tradinghub.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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

import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.application.service.order.OrderQueryService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderStatus;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.user.User;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderQueryService orderQueryService;

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
    @DisplayName("사용자 ID로 모든 주문 조회 성공")
    void getOrdersByUserId_Success() {
        // GIVEN
        Order order1 = createOrder(OrderType.MARKET, OrderStatus.FILLED);
        Order order2 = createOrder(OrderType.LIMIT, OrderStatus.PENDING);
        List<Order> expectedOrders = Arrays.asList(order1, order2);
        
        given(orderRepository.findByUserIdOrderByCreatedAtDesc(userId)).willReturn(expectedOrders);

        // WHEN
        List<Order> orders = orderQueryService.getOrdersByUserId(userId);

        // THEN
        then(orderRepository).should(times(1)).findByUserIdOrderByCreatedAtDesc(userId);
        assertThat(orders).isEqualTo(expectedOrders);
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID로 완료된 주문 조회 성공")
    void getCompletedOrdersByUserId_Success() {
        // GIVEN
        Order filledOrder = createOrder(OrderType.MARKET, OrderStatus.FILLED);
        Order cancelledOrder = createOrder(OrderType.LIMIT, OrderStatus.CANCELLED);
        List<Order> expectedOrders = Arrays.asList(filledOrder, cancelledOrder);
        
        given(orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, 
                Arrays.asList(OrderStatus.FILLED, OrderStatus.CANCELLED)))
            .willReturn(expectedOrders);

        // WHEN
        List<Order> orders = orderQueryService.getCompletedOrdersByUserId(userId);

        // THEN
        then(orderRepository).should(times(1))
            .findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, 
                Arrays.asList(OrderStatus.FILLED, OrderStatus.CANCELLED));
        assertThat(orders).isEqualTo(expectedOrders);
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("사용자 ID와 심볼로 주문 조회 성공")
    void getOrdersByUserIdAndSymbol_Success() {
        // GIVEN
        Order order1 = createOrder(OrderType.MARKET, OrderStatus.FILLED);
        Order order2 = createOrder(OrderType.LIMIT, OrderStatus.PENDING);
        List<Order> expectedOrders = Arrays.asList(order1, order2);
        
        given(orderRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(userId, symbol))
            .willReturn(expectedOrders);

        // WHEN
        List<Order> orders = orderQueryService.getOrdersByUserIdAndSymbol(userId, symbol);

        // THEN
        then(orderRepository).should(times(1))
            .findByUserIdAndSymbolOrderByCreatedAtDesc(userId, symbol);
        assertThat(orders).isEqualTo(expectedOrders);
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("주문 ID로 주문 조회 성공")
    void getOrderById_Success() {
        // GIVEN
        Order expectedOrder = createOrder(OrderType.MARKET, OrderStatus.FILLED);
        
        given(orderRepository.findById(orderId)).willReturn(Optional.of(expectedOrder));

        // WHEN
        Order order = orderQueryService.getOrderById(orderId);

        // THEN
        then(orderRepository).should(times(1)).findById(orderId);
        assertThat(order).isEqualTo(expectedOrder);
    }

    @Test
    @DisplayName("주문 ID로 주문 조회 실패 - 주문 없음")
    void getOrderById_Failure_OrderNotFound() {
        // GIVEN
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // WHEN & THEN
        assertThatThrownBy(() -> orderQueryService.getOrderById(orderId))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found with id: " + orderId);

        then(orderRepository).should(times(1)).findById(orderId);
    }

    private Order createOrder(OrderType type, OrderStatus status) {
        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(type)
                .side(side)
                .price(price)
                .amount(amount)
                .status(status)
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }
} 