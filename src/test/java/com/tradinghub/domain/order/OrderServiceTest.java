package com.tradinghub.domain.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.tradinghub.domain.order.dto.OrderRequest;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PortfolioService portfolioService;
    
    @Mock
    private OrderWebSocketHandler webSocketHandler;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;
    
    @InjectMocks
    private OrderService orderService;
    
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
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        
        // 테스트용 포트폴리오 모의 객체 (BUY 주문에 필요한 잔고 확인)
        Portfolio portfolio = mock(Portfolio.class);
        when(portfolio.getUsdBalance()).thenReturn(new BigDecimal("100000.00"));
        when(portfolioService.getPortfolio(anyLong())).thenReturn(portfolio);
        
        Order savedOrder = Order.builder()
            .user(testUser)
            .symbol("BTC")
            .type(Order.OrderType.LIMIT)
            .side(Order.OrderSide.BUY)
            .price(new BigDecimal("50000.00"))
            .amount(new BigDecimal("0.5"))
            .status(Order.OrderStatus.PENDING)
            .build();
        
        // OrderRepository.save() 메소드가 호출될 때 savedOrder를 반환하도록 설정
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        doNothing().when(webSocketHandler).notifyNewOrder(any(Order.class));
        
        // when
        Order result = orderService.createLimitOrder(
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
        
        verify(userRepository).findById(1L);
        verify(portfolioService).getPortfolio(1L);
        verify(portfolio).getUsdBalance();
        verify(orderRepository).save(any(Order.class));
        verify(webSocketHandler).notifyNewOrder(any(Order.class));
    }
    
    @Test
    @DisplayName("사용자가 존재하지 않을 때 지정가 주문 생성 실패 테스트")
    void createLimitOrder_userNotFound() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());
        
        // when & then
        assertThrows(RuntimeException.class, () -> {
            orderService.createLimitOrder(
                1L, 
                orderRequest.getSymbol(), 
                orderRequest.getSide(), 
                orderRequest.getPrice(), 
                orderRequest.getAmount()
            );
        });
        
        verify(userRepository).findById(1L);
        verifyNoInteractions(orderRepository, webSocketHandler, portfolioService);
    }
} 