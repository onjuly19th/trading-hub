package com.tradinghub.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.application.service.order.OrderQueryService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderStatus;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.model.user.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderQueryServiceIntegrationTest {

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Long userId;
    private String symbol = "BTCUSDT";
    private BigDecimal price = new BigDecimal("50000.00");
    private BigDecimal amount = new BigDecimal("0.5");
    private OrderSide side = OrderSide.BUY;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setPassword("password");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
        userId = testUser.getId();

        // 기존 주문 데이터 삭제
        orderRepository.deleteAll();

        // 테스트용 주문 데이터 생성
        Order marketOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.FILLED)
                .build();
        orderRepository.save(marketOrder);

        Order limitOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        orderRepository.save(limitOrder);
    }

    @Test
    @DisplayName("사용자 ID로 모든 주문 조회 성공 - 실제 DB")
    void getOrdersByUserId_Success() {
        // WHEN
        List<Order> orders = orderQueryService.getOrdersByUserId(userId);

        // THEN
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting("type")
                .containsExactlyInAnyOrder(OrderType.MARKET, OrderType.LIMIT);
    }

    @Test
    @DisplayName("사용자 ID로 완료된 주문 조회 성공 - 실제 DB")
    void getCompletedOrdersByUserId_Success() {
        // WHEN
        List<Order> orders = orderQueryService.getCompletedOrdersByUserId(userId);

        // THEN
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    @DisplayName("사용자 ID와 심볼로 주문 조회 성공 - 실제 DB")
    void getOrdersByUserIdAndSymbol_Success() {
        // WHEN
        List<Order> orders = orderQueryService.getOrdersByUserIdAndSymbol(userId, symbol);

        // THEN
        assertThat(orders).hasSize(2);
        assertThat(orders).extracting("symbol").containsOnly(symbol);
    }

    @Test
    @DisplayName("주문 ID로 주문 조회 성공 - 실제 DB")
    void getOrderById_Success() {
        // GIVEN
        Order savedOrder = orderRepository.findAll().get(0);
        Long orderId = savedOrder.getId();

        // WHEN
        Order order = orderQueryService.getOrderById(orderId);

        // THEN
        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("주문 ID로 주문 조회 실패 - 주문 없음 - 실제 DB")
    void getOrderById_Failure_OrderNotFound() {
        // WHEN & THEN
        assertThatThrownBy(() -> orderQueryService.getOrderById(999999L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order not found with id: 999999");
    }
} 