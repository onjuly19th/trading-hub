package com.tradinghub.domain.trading;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.common.exception.auth.UnauthorizedOperationException;
import com.tradinghub.common.exception.order.InvalidOrderException;
import com.tradinghub.common.exception.order.OrderNotFoundException;
import com.tradinghub.common.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.trading.dto.OrderExecutionRequest;
import com.tradinghub.domain.trading.event.OrderExecutedEvent;
import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;
import com.tradinghub.infrastructure.aop.LogExecutionTime;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService; // 주문 검증 및 포트폴리오 조회용으로만 사용
    private final OrderWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;

    /*
     * 시장가 주문 생성 후 즉시 체결
     */
    @LogExecutionTime
    @Transactional
    public Order createMarketOrder(Long userId, String symbol, Order.OrderSide side,
                                   BigDecimal price, BigDecimal amount) {
        // 사용자 정보 조회 및 주문 가능 여부 확인
        User user = getUser(userId);
        validateOrder(user, side, price, amount);

        // 주문 생성 및 저장
        Order order = createOrderEntity(user, symbol, side, Order.OrderType.MARKET, price, amount);
        order.setStatus(Order.OrderStatus.FILLED); // 시장가 주문은 즉시 체결 상태로 설정
        order.setExecutedPrice(price);
        order = orderRepository.save(order);
        
        // 이벤트 발행 및 WebSocket 알림
        updatePortfolioForOrder(userId, order);
        webSocketHandler.notifyNewOrder(order);
        
        return order;
    }
    
    /**
     * 주문 정보를 기반으로 이벤트 발행
     * 직접적인 포트폴리오 업데이트 대신 이벤트를 통해 느슨하게 결합
     */
    @LogExecutionTime
    private void updatePortfolioForOrder(Long userId, Order order) {
        // 이벤트 발행 - 포트폴리오 업데이트는 리스너에서 처리
        eventPublisher.publishEvent(new OrderExecutedEvent(order));
    }

    /**
     * 지정가 주문 생성
     * Order 테이블에 저장, 가격 조건 충족 시 체결
     */
    @LogExecutionTime
    @Transactional
    public Order createLimitOrder(Long userId, String symbol, Order.OrderSide side, 
                                  BigDecimal price, BigDecimal amount) {
        // 사용자 정보 조회 및 주문 가능 여부 확인
        User user = getUser(userId);
        validateOrder(user, side, price, amount);

        // 주문 생성 및 저장
        Order order = createOrderEntity(user, symbol, side, Order.OrderType.LIMIT, price, amount);
        order.setStatus(Order.OrderStatus.PENDING);
        order = orderRepository.save(order);
        
        // WebSocket 알림
        webSocketHandler.notifyNewOrder(order);

        return order;
    }
    
    /**
     * 주문 엔티티 생성 공통 로직
     */
    private Order createOrderEntity(User user, String symbol, Order.OrderSide side, 
                                   Order.OrderType type, BigDecimal price, BigDecimal amount) {
        return Order.builder()
            .user(user)
            .symbol(symbol)
            .side(side)
            .type(type)
            .price(price)
            .amount(amount)
            .build();
    }
    
    /**
     * 사용자 정보 조회
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new InvalidOrderException("User not found"));
    }

    @LogExecutionTime
    @Transactional
    public int executeOrdersAtPrice(String symbol, BigDecimal currentPrice) {
        List<Order> executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        
        if (executableOrders.isEmpty()) {
            return 0;
        }

        int executedCount = 0;
        for (Order order : executableOrders) {
            boolean success = executeOrder(order);
            if (success) {
                executedCount++;
            }
        }

        return executedCount;
    }
    
    /**
     * 개별 주문 체결 처리
     * @param order 체결할 주문
     * @return 체결 성공 여부
     */
    @LogExecutionTime
    @Transactional
    public boolean executeOrder(Order order) {
        try {
            // 주문 상태를 체결로 변경
            order.fill();
            orderRepository.save(order);

            // 포트폴리오 업데이트
            OrderExecutionRequest request = OrderExecutionRequest.from(order);
            portfolioService.updatePortfolioForOrder(order.getUser().getId(), request);

            return true;
        } catch (Exception e) {
            log.error("주문 체결 실패: {}", e.getMessage());
            return false;
        }
    }

    private void validateOrder(User user, Order.OrderSide side, BigDecimal price, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderException("Order amount must be greater than zero");
        }

        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        
        if (side == Order.OrderSide.BUY) {
            // 매수 시 USD 잔고 확인
            BigDecimal requiredUsd = price.multiply(amount);
            if (portfolio.getUsdBalance().compareTo(requiredUsd) < 0) {
                throw new InsufficientBalanceException(
                    String.format("Insufficient USD balance. Required: %s, Available: %s", 
                        requiredUsd, portfolio.getUsdBalance())
                );
            }
        } else {
            // 매도 시 코인 잔고 확인
            if (portfolio.getCoinBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(
                    String.format("Insufficient coin balance. Required: %s, Available: %s", 
                        amount, portfolio.getCoinBalance())
                );
            }
        }
    }

    @LogExecutionTime
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedOperationException("order cancellation");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new InvalidOrderException("Cannot cancel order with status: " + order.getStatus());
        }

        order.cancel();
        order = orderRepository.save(order);
        webSocketHandler.notifyOrderUpdate(order);
    }
    
    /**
     * 사용자 ID로 모든 주문 조회
     */
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 사용자 ID로 완료된 주문 조회 (체결 또는 취소)
     */
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<Order> getCompletedOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
            userId, List.of(Order.OrderStatus.FILLED, Order.OrderStatus.CANCELLED));
    }
    
    /**
     * 사용자 ID와 심볼로 주문 조회
     */
    @LogExecutionTime
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserIdAndSymbol(Long userId, String symbol) {
        return orderRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(userId, symbol);
    }
} 