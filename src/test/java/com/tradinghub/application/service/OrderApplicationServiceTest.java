package com.tradinghub.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.tradinghub.application.service.order.OrderCommandService;
import com.tradinghub.application.service.portfolio.PortfolioService;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderApplicationService;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.interfaces.dto.order.OrderRequest;
import com.tradinghub.interfaces.exception.order.InvalidOrderException;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

@ExtendWith(MockitoExtension.class)
public class OrderApplicationServiceTest {

    @Mock
    private OrderCommandService orderCommandService;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PortfolioService portfolioService;
    
    @Mock
    private OrderWebSocketHandler webSocketHandler;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderApplicationService orderApplicationService;
    
    private User testUser;
    private OrderRequest orderRequest;
    
    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        
        // 테스트용 주문 요청 생성
        orderRequest = new OrderRequest();
        orderRequest.setSymbol("BTC");
        orderRequest.setType(Order.OrderType.LIMIT);
        orderRequest.setSide(Order.OrderSide.BUY);
        orderRequest.setPrice(new BigDecimal("50000.00"));
        orderRequest.setAmount(new BigDecimal("0.5"));
    }
    
    @Test
    @DisplayName("지정가 주문 생성 성공 테스트")
    void createLimitOrder_success() {
        // given
        Order savedOrder = Order.builder()
            .user(testUser)
            .symbol("BTC")
            .type(Order.OrderType.LIMIT)
            .side(Order.OrderSide.BUY)
            .price(new BigDecimal("50000.00"))
            .amount(new BigDecimal("0.5"))
            .status(Order.OrderStatus.PENDING)
            .build();
        
        // OrderCommandService.createLimitOrder() 메소드가 호출될 때 savedOrder를 반환하도록 설정
        when(orderCommandService.createLimitOrder(
            anyLong(), 
            any(String.class), 
            any(Order.OrderSide.class), 
            any(BigDecimal.class), 
            any(BigDecimal.class)
        )).thenReturn(savedOrder);
        
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));
        
        // when
        Order result = orderApplicationService.createLimitOrder(
            1L, 
            orderRequest.getSymbol(), 
            orderRequest.getSide(), 
            orderRequest.getPrice(), 
            orderRequest.getAmount()
        );
        
        // then
        assertNotNull(result);
        assertEquals("BTC", result.getSymbol());
        assertEquals(Order.OrderType.LIMIT, result.getType());
        assertEquals(Order.OrderSide.BUY, result.getSide());
        assertEquals(0, new BigDecimal("50000.00").compareTo(result.getPrice()));
        assertEquals(0, new BigDecimal("0.5").compareTo(result.getAmount()));
        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
        
        verify(orderCommandService).createLimitOrder(
            1L, 
            orderRequest.getSymbol(),
            orderRequest.getSide(),
            orderRequest.getPrice(),
            orderRequest.getAmount()
        );
        verify(webSocketHandler).notifyNewOrder(savedOrder);
    }
    
    @Test
    @DisplayName("OrderCommandService에서 예외 발생시 주문 생성 실패 테스트")
    void createLimitOrder_serviceError() {
        // given
        when(orderCommandService.createLimitOrder(
            anyLong(), 
            any(String.class), 
            any(Order.OrderSide.class), 
            any(BigDecimal.class), 
            any(BigDecimal.class)
        )).thenThrow(new InvalidOrderException("Invalid order"));
        
        // when & then
        assertThrows(InvalidOrderException.class, () -> {
            orderApplicationService.createLimitOrder(
                1L, 
                orderRequest.getSymbol(), 
                orderRequest.getSide(), 
                orderRequest.getPrice(), 
                orderRequest.getAmount()
            );
        });
        
        verify(orderCommandService).createLimitOrder(
            1L, 
            orderRequest.getSymbol(),
            orderRequest.getSide(),
            orderRequest.getPrice(),
            orderRequest.getAmount()
        );
        verifyNoInteractions(webSocketHandler);
    }
} 