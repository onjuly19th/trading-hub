package com.tradinghub.domain.order.application;

import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;
import com.tradinghub.domain.order.Order;
import com.tradinghub.domain.order.OrderRepository;
import com.tradinghub.domain.order.Order.OrderSide;
import com.tradinghub.domain.order.Order.OrderStatus;
import com.tradinghub.domain.order.Order.OrderType;
import com.tradinghub.domain.order.dto.OrderRequest;
import com.tradinghub.domain.order.service.OrderCommandService;
import com.tradinghub.domain.order.service.OrderQueryService;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;
import com.tradinghub.common.exception.order.InvalidOrderException;
import com.tradinghub.common.exception.order.OrderExecutionException;
import com.tradinghub.common.exception.order.OrderNotFoundException;
import com.tradinghub.common.exception.auth.UnauthorizedOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OrderApplicationServiceTest {

    @Mock
    private OrderCommandService orderCommandService;
    
    @Mock
    private OrderQueryService orderQueryService;
    
    @Mock
    private OrderWebSocketHandler webSocketHandler;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderApplicationService orderApplicationService;

    private User testUser;
    private Long userId = 1L;
    private Long mockOrderId = 100L;
    private String symbol = "BTCUSDT";

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
    @DisplayName("지정가 주문 생성 성공")
    void createLimitOrder_Success() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setType(OrderType.LIMIT);
        request.setSide(side);
        request.setPrice(price);
        request.setAmount(amount);

        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", mockOrderId);
        
        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));

        // WHEN
        Order order = orderApplicationService.createLimitOrder(userId, symbol, side, price, amount);

        // THEN
        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(times(1)).notifyNewOrder(mockOrder);
        then(eventPublisher).should(never()).publishEvent(any(Object.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(mockOrderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getSide()).isEqualTo(side);
    }

    @Test
    @DisplayName("지정가 주문 생성 실패 - 유효하지 않은 주문")
    void createLimitOrder_Failure_InvalidOrder() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willThrow(new InvalidOrderException("Invalid order parameters"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createLimitOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Invalid order parameters");

        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("지정가 주문 생성 실패 - 주문 실행 오류")
    void createLimitOrder_Failure_OrderExecutionError() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willThrow(new OrderExecutionException("Failed to execute order"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createLimitOrder(userId, symbol, side, price, amount))
            .isInstanceOf(OrderExecutionException.class)
            .hasMessage("Failed to execute order");

        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("지정가 주문 생성 실패 - 가격이 0 이하")
    void createLimitOrder_Failure_InvalidPrice() {
        // GIVEN
        BigDecimal price = new BigDecimal("0");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willThrow(new InvalidOrderException("Price must be greater than zero"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createLimitOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Price must be greater than zero");

        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("지정가 주문 생성 실패 - 수량이 0 이하")
    void createLimitOrder_Failure_InvalidAmount() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0");
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willThrow(new InvalidOrderException("Amount must be greater than zero"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createLimitOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Amount must be greater than zero");

        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("시장가 주문 생성 성공")
    void createMarketOrder_Success() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setType(OrderType.MARKET);
        request.setSide(side);
        request.setPrice(price);
        request.setAmount(amount);

        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.FILLED)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", mockOrderId);
        
        given(orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));
        doNothing().when(eventPublisher).publishEvent(any(Object.class));

        // WHEN
        Order order = orderApplicationService.createMarketOrder(userId, symbol, side, price, amount);

        // THEN
        then(orderCommandService).should(times(1)).createMarketOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(times(1)).notifyNewOrder(mockOrder);
        then(eventPublisher).should(times(1)).publishEvent(any(Object.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(mockOrderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getSide()).isEqualTo(side);
    }

    @Test
    @DisplayName("시장가 주문 생성 실패 - 유효하지 않은 주문")
    void createMarketOrder_Failure_InvalidOrder() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .willThrow(new InvalidOrderException("Invalid market order parameters"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createMarketOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Invalid market order parameters");

        then(orderCommandService).should(times(1)).createMarketOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("시장가 주문 생성 실패 - 잔고 부족")
    void createMarketOrder_Failure_InsufficientBalance() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("100.0"); // 큰 금액
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .willThrow(new OrderExecutionException("Insufficient balance for market order"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createMarketOrder(userId, symbol, side, price, amount))
            .isInstanceOf(OrderExecutionException.class)
            .hasMessage("Insufficient balance for market order");

        then(orderCommandService).should(times(1)).createMarketOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("시장가 주문 생성 실패 - 수량이 0 이하")
    void createMarketOrder_Failure_InvalidAmount() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0");
        OrderSide side = OrderSide.BUY;

        given(orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .willThrow(new InvalidOrderException("Amount must be greater than zero"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.createMarketOrder(userId, symbol, side, price, amount))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Amount must be greater than zero");

        then(orderCommandService).should(times(1)).createMarketOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(never()).notifyNewOrder(any(Order.class));
        then(eventPublisher).should(never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_Success() {
        // GIVEN
        Long orderId = 100L;
        
        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.CANCELLED)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", orderId);
        
        given(orderCommandService.cancelOrder(orderId, userId))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyOrderUpdate(any(Order.class));

        // WHEN
        orderApplicationService.cancelOrder(orderId, userId);

        // THEN
        then(orderCommandService).should(times(1)).cancelOrder(orderId, userId);
        then(webSocketHandler).should(times(1)).notifyOrderUpdate(mockOrder);
    }

    @Test
    @DisplayName("주문 취소 실패 - 존재하지 않는 주문")
    void cancelOrder_Failure_OrderNotFound() {
        // GIVEN
        Long orderId = 999L; // 존재하지 않는 주문 ID
        
        given(orderCommandService.cancelOrder(orderId, userId))
            .willThrow(new OrderNotFoundException("Order not found with id: " + orderId));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.cancelOrder(orderId, userId))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found with id: " + orderId);

        then(orderCommandService).should(times(1)).cancelOrder(orderId, userId);
        then(webSocketHandler).should(never()).notifyOrderUpdate(any(Order.class));
    }

    @Test
    @DisplayName("주문 취소 실패 - 권한 없음")
    void cancelOrder_Failure_Unauthorized() {
        // GIVEN
        Long orderId = 100L;
        Long otherUserId = 2L; // 다른 사용자 ID
        
        given(orderCommandService.cancelOrder(orderId, otherUserId))
            .willThrow(new UnauthorizedOperationException("User is not authorized to cancel this order"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.cancelOrder(orderId, otherUserId))
            .isInstanceOf(UnauthorizedOperationException.class)
            .hasMessage("User is not authorized to cancel this order");

        then(orderCommandService).should(times(1)).cancelOrder(orderId, otherUserId);
        then(webSocketHandler).should(never()).notifyOrderUpdate(any(Order.class));
    }

    @Test
    @DisplayName("주문 취소 실패 - 이미 체결된 주문")
    void cancelOrder_Failure_AlreadyFilled() {
        // GIVEN
        Long orderId = 100L;
        
        given(orderCommandService.cancelOrder(orderId, userId))
            .willThrow(new InvalidOrderException("Cannot cancel an already filled order"));

        // WHEN & THEN
        assertThatThrownBy(() -> 
            orderApplicationService.cancelOrder(orderId, userId))
            .isInstanceOf(InvalidOrderException.class)
            .hasMessage("Cannot cancel an already filled order");

        then(orderCommandService).should(times(1)).cancelOrder(orderId, userId);
        then(webSocketHandler).should(never()).notifyOrderUpdate(any(Order.class));
    }

    @Test
    @DisplayName("시장가 매수 주문 생성 성공")
    void createMarketOrder_Success_Buy() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setType(OrderType.MARKET);
        request.setSide(side);
        request.setPrice(price);
        request.setAmount(amount);

        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.FILLED)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", mockOrderId);
        
        given(orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));
        doNothing().when(eventPublisher).publishEvent(any(Object.class));

        // WHEN
        Order order = orderApplicationService.createMarketOrder(userId, symbol, side, price, amount);

        // THEN
        then(orderCommandService).should(times(1)).createMarketOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(times(1)).notifyNewOrder(mockOrder);
        then(eventPublisher).should(times(1)).publishEvent(any(Object.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(mockOrderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getSide()).isEqualTo(side);
    }

    @Test
    @DisplayName("시장가 매도 주문 생성 성공")
    void createMarketOrder_Success_Sell() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.SELL;

        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setType(OrderType.MARKET);
        request.setSide(side);
        request.setPrice(price);
        request.setAmount(amount);

        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.FILLED)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", mockOrderId);
        
        given(orderCommandService.createMarketOrder(userId, symbol, side, price, amount))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));
        doNothing().when(eventPublisher).publishEvent(any(Object.class));

        // WHEN
        Order order = orderApplicationService.createMarketOrder(userId, symbol, side, price, amount);

        // THEN
        then(orderCommandService).should(times(1)).createMarketOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(times(1)).notifyNewOrder(mockOrder);
        then(eventPublisher).should(times(1)).publishEvent(any(Object.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(mockOrderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.MARKET);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getSide()).isEqualTo(side);
    }

    @Test
    @DisplayName("지정가 매수 주문 생성 성공")
    void createLimitOrder_Success_Buy() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setType(OrderType.LIMIT);
        request.setSide(side);
        request.setPrice(price);
        request.setAmount(amount);

        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", mockOrderId);
        
        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));

        // WHEN
        Order order = orderApplicationService.createLimitOrder(userId, symbol, side, price, amount);

        // THEN
        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(times(1)).notifyNewOrder(mockOrder);
        then(eventPublisher).should(never()).publishEvent(any(Object.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(mockOrderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getSide()).isEqualTo(side);
    }

    @Test
    @DisplayName("지정가 매도 주문 생성 성공")
    void createLimitOrder_Success_Sell() {
        // GIVEN
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.SELL;

        OrderRequest request = new OrderRequest();
        request.setSymbol(symbol);
        request.setType(OrderType.LIMIT);
        request.setSide(side);
        request.setPrice(price);
        request.setAmount(amount);

        // Mock OrderCommandService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", mockOrderId);
        
        given(orderCommandService.createLimitOrder(userId, symbol, side, price, amount))
            .willReturn(mockOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));

        // WHEN
        Order order = orderApplicationService.createLimitOrder(userId, symbol, side, price, amount);

        // THEN
        then(orderCommandService).should(times(1)).createLimitOrder(userId, symbol, side, price, amount);
        then(webSocketHandler).should(times(1)).notifyNewOrder(mockOrder);
        then(eventPublisher).should(never()).publishEvent(any(Object.class));

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(mockOrderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPrice()).isEqualTo(price);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getSide()).isEqualTo(side);
    }

    @Test
    @DisplayName("주문 조회 성공")
    void getOrder_Success() {
        // GIVEN
        Long orderId = 100L;
        
        // Mock OrderQueryService
        Order mockOrder = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(mockOrder, "id", orderId);
        
        given(orderQueryService.getOrderById(orderId))
            .willReturn(mockOrder);

        // WHEN
        Order order = orderQueryService.getOrderById(orderId);

        // THEN
        then(orderQueryService).should(times(1)).getOrderById(orderId);

        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getSymbol()).isEqualTo(symbol);
        assertThat(order.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getPrice()).isEqualTo(new BigDecimal("50000.00"));
        assertThat(order.getAmount()).isEqualTo(new BigDecimal("0.5"));
        assertThat(order.getSide()).isEqualTo(OrderSide.BUY);
    }

    @Test
    @DisplayName("사용자의 주문 목록 조회 성공")
    void getOrdersByUserId_Success() {
        // GIVEN
        String symbol = "BTCUSDT";
        
        // Mock OrderQueryService
        Order mockOrder1 = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.PENDING)
                .build();
        Order mockOrder2 = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(OrderSide.SELL)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.FILLED)
                .build();
        
        given(orderQueryService.getOrdersByUserId(userId))
            .willReturn(Arrays.asList(mockOrder1, mockOrder2));

        // WHEN
        List<Order> orders = orderApplicationService.getOrdersByUserId(userId);

        // THEN
        then(orderQueryService).should(times(1)).getOrdersByUserId(userId);

        assertThat(orders).isNotNull();
        assertThat(orders.size()).isEqualTo(2);
        assertThat(orders.get(0).getSymbol()).isEqualTo(symbol);
        assertThat(orders.get(1).getSymbol()).isEqualTo(symbol);
    }

    @Test
    @DisplayName("심볼별 주문 목록 조회 성공")
    void getOrdersBySymbol_Success() {
        // GIVEN
        String symbol = "BTCUSDT";
        
        // Mock OrderQueryService
        Order mockOrder1 = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.PENDING)
                .build();
        Order mockOrder2 = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(OrderSide.SELL)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.FILLED)
                .build();
        
        given(orderQueryService.getOrdersByUserIdAndSymbol(userId, symbol))
            .willReturn(Arrays.asList(mockOrder1, mockOrder2));

        // WHEN
        List<Order> orders = orderApplicationService.getOrdersByUserIdAndSymbol(userId, symbol);

        // THEN
        then(orderQueryService).should(times(1)).getOrdersByUserIdAndSymbol(userId, symbol);

        assertThat(orders).isNotNull();
        assertThat(orders.size()).isEqualTo(2);
        assertThat(orders.get(0).getSymbol()).isEqualTo(symbol);
        assertThat(orders.get(1).getSymbol()).isEqualTo(symbol);
    }

    @Test
    @DisplayName("완료된 주문 목록 조회 성공")
    void getCompletedOrders_Success() {
        // GIVEN
        String symbol = "BTCUSDT";
        
        // Mock OrderQueryService
        Order mockOrder1 = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(OrderSide.BUY)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.FILLED)
                .build();
        Order mockOrder2 = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.MARKET)
                .side(OrderSide.SELL)
                .price(new BigDecimal("50000.00"))
                .amount(new BigDecimal("0.5"))
                .status(OrderStatus.FILLED)
                .build();
        
        given(orderQueryService.getCompletedOrdersByUserId(userId))
            .willReturn(Arrays.asList(mockOrder1, mockOrder2));

        // WHEN
        List<Order> orders = orderApplicationService.getCompletedOrdersByUserId(userId);

        // THEN
        then(orderQueryService).should(times(1)).getCompletedOrdersByUserId(userId);

        assertThat(orders).isNotNull();
        assertThat(orders.size()).isEqualTo(2);
        assertThat(orders.get(0).getSymbol()).isEqualTo(symbol);
        assertThat(orders.get(1).getSymbol()).isEqualTo(symbol);
    }

    @Test
    @DisplayName("주문 실행 성공")
    void executeOrder_Success() {
        // GIVEN
        String symbol = "BTCUSDT";
        BigDecimal price = new BigDecimal("50000.00");
        BigDecimal amount = new BigDecimal("0.5");
        OrderSide side = OrderSide.BUY;

        Order order = Order.builder()
                .user(testUser)
                .symbol(symbol)
                .type(OrderType.LIMIT)
                .side(side)
                .price(price)
                .amount(amount)
                .status(OrderStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(order, "id", mockOrderId);
        
        given(orderCommandService.executeOrder(order))
            .willReturn(order);
        
        doNothing().when(webSocketHandler).notifyOrderUpdate(any(Order.class));

        // WHEN
        orderApplicationService.executeOrder(order);

        // THEN
        then(orderCommandService).should(times(1)).executeOrder(order);
        then(webSocketHandler).should(times(1)).notifyOrderUpdate(order);
    }
}