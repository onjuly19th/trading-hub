package com.tradinghub.domain.trading;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.domain.user.User;
import com.tradinghub.domain.user.UserRepository;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.trading.dto.OrderExecutionRequest;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PortfolioService portfolioService;
    private final OrderWebSocketHandler webSocketHandler;

    /*
     * 시장가 주문 생성 후 즉시 체결
     */
    @Transactional
    public Order createMarketOrder(Long userId, String symbol, Order.OrderSide side,
                                   BigDecimal price, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal currentPrice = price;
        
        // 주문 가능 여부 확인
        validateOrder(user, side, currentPrice, amount);

        // 시장가 주문 생성
        Order order = Order.builder()
            .user(user)
            .symbol(symbol)
            .side(side)
            .type(Order.OrderType.MARKET) // 시장가 주문 타입
            .amount(amount)
            .price(currentPrice)
            .status(Order.OrderStatus.FILLED) // 바로 체결 상태로 설정
            .build();
        
        // 체결 가격 설정
        order.setExecutedPrice(currentPrice);
        
        // 주문 저장
        order = orderRepository.save(order);
        
        // 포트폴리오 업데이트
        updatePortfolioForOrder(user.getId(), order);
        
        // WebSocket을 통한 실시간 알림
        webSocketHandler.notifyNewOrder(order);
        
        // 포트폴리오 업데이트 알림 추가
        Portfolio portfolio = portfolioService.getPortfolio(userId);
        webSocketHandler.notifyPortfolioUpdate(portfolio);

        return order;
    }
    
    /**
     * 주문 정보를 기반으로 포트폴리오 업데이트
     */
    private void updatePortfolioForOrder(Long userId, Order order) {
        // OrderExecutionRequest를 사용하여 포트폴리오 업데이트
        OrderExecutionRequest executionRequest = OrderExecutionRequest.from(order);
        portfolioService.updatePortfolioForOrder(userId, executionRequest);
    }

    /**
     * 지정가 주문 생성
     * Order 테이블에 저장, 가격 조건 충족 시 체결
     */
    @Transactional
    public Order createLimitOrder(Long userId, String symbol, Order.OrderSide side, 
                                  BigDecimal price, BigDecimal amount) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 주문 가능 여부 확인
        validateOrder(user, side, price, amount);

        Order order = Order.builder()
            .user(user)
            .symbol(symbol)
            .type(Order.OrderType.LIMIT)
            .side(side)
            .price(price)
            .amount(amount)
            .status(Order.OrderStatus.PENDING)
            .build();

        order = orderRepository.save(order);
        
        // WebSocket을 통한 실시간 알림
        webSocketHandler.notifyNewOrder(order);

        return order;
    }

    @Transactional
    public int executeOrdersAtPrice(String symbol, BigDecimal currentPrice) {
        List<Order> executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        
        if (executableOrders.isEmpty()) {
            return 0;
        }

        log.info("Found {} executable orders for symbol {} at price {}", 
            executableOrders.size(), symbol, currentPrice);

        int executedCount = 0;
        for (Order order : executableOrders) {
            try {
                // 주문 상태 업데이트
                order.fill();
                order.setExecutedPrice(currentPrice);
                orderRepository.save(order);
                
                // 포트폴리오 업데이트
                updatePortfolioForOrder(order.getUser().getId(), order);
                
                // WebSocket을 통한 실시간 알림
                webSocketHandler.notifyOrderUpdate(order);
                
                // 포트폴리오 업데이트 알림 추가
                Portfolio portfolio = portfolioService.getPortfolio(order.getUser().getId());
                webSocketHandler.notifyPortfolioUpdate(portfolio);

                log.info("Order executed: ID={}, Type={}, Amount={}, Price={}", 
                    order.getId(), order.getType(), order.getAmount(), currentPrice);
                
                executedCount++;
            } catch (Exception e) {
                log.error("Failed to execute order {}: {}", order.getId(), e.getMessage());
                order.setStatus(Order.OrderStatus.FAILED);
                order = orderRepository.save(order);
                
                // 실패한 주문도 상태 업데이트 알림
                webSocketHandler.notifyOrderUpdate(order);
            }
        }

        return executedCount;
    }

    private void validateOrder(User user, Order.OrderSide side, BigDecimal price, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount");
        }

        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        
        if (side == Order.OrderSide.BUY) {
            // 매수 시 USD 잔고 확인
            BigDecimal requiredUsd = price.multiply(amount);
            if (portfolio.getUsdBalance().compareTo(requiredUsd) < 0) {
                throw new RuntimeException("Insufficient USD balance");
            }
        } else {
            // 매도 시 코인 잔고 확인
            if (portfolio.getCoinBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient coin balance");
            }
        }
    }

    // FE 미구현
    /*
    @Transactional(readOnly = true)
    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserIdAndStatus(userId, Order.OrderStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrderBook(String symbol) {
        return orderRepository.findBySymbolAndStatusOrderByPriceDesc(symbol, Order.OrderStatus.PENDING);
    }
    */

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this order");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Order cannot be cancelled");
        }

        order.cancel();
        order = orderRepository.save(order);
        
        // 취소된 주문 알림
        webSocketHandler.notifyOrderUpdate(order);
    }
} 