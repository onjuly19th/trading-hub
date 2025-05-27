package com.tradinghub.application.service.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.order.InvalidOrderException;
import com.tradinghub.application.exception.order.OrderExecutionException;
import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.model.user.UserRepository;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;

import lombok.RequiredArgsConstructor;

/**
 * 주문 생성, 취소, 실행 등 상태 변경을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class OrderCommandService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderValidator orderValidator;
    
    /**
     * 시장가 주문을 생성합니다.
     *
     * @param userId 주문 생성 사용자 ID
     * @param symbol 거래 심볼
     * @param side   매수/매도 구분
     * @param price  주문 가격
     * @param amount 주문 수량
     * @return 생성된 주문 객체
     */
    @Transactional
    public Order createMarketOrder(Long userId, String symbol, Order.OrderSide side,
                                  BigDecimal price, BigDecimal amount) {
        User user = getUser(userId);
        orderValidator.validateOrderCreation(user, side, price, amount);
        
        Order order = Order.builder()
                .user(user)
                .symbol(symbol)
                .side(side)
                .type(Order.OrderType.MARKET)
                .price(price)
                .amount(amount)
                .status(Order.OrderStatus.FILLED)
                .build();
                
        order.setExecutedPrice(price);
        return orderRepository.save(order);
    }
    
    /**
     * 지정가 주문을 생성합니다.
     *
     * @param userId 주문 생성 사용자 ID
     * @param symbol 거래 심볼
     * @param side   매수/매도 구분
     * @param price  지정 가격
     * @param amount 주문 수량
     * @return 생성된 주문 객체
     */
    @Transactional
    public Order createLimitOrder(Long userId, String symbol, Order.OrderSide side,
                                 BigDecimal price, BigDecimal amount) {
        User user = getUser(userId);
        orderValidator.validateOrderCreation(user, side, price, amount);
        
        Order order = Order.builder()
                .user(user)
                .symbol(symbol)
                .side(side)
                .type(Order.OrderType.LIMIT)
                .price(price)
                .amount(amount)
                .status(Order.OrderStatus.PENDING)
                .build();
                
        return orderRepository.save(order);
    }
    
    /**
     * 개별 주문을 체결 상태로 변경합니다.
     *
     * @param order 체결할 주문
     * @return 체결된 주문
     */
    @Transactional
    public Order executeOrder(Order order) {
        order.fill();
        return orderRepository.save(order);
    }
    
    /**
     * 주문을 취소 상태로 변경합니다.
     *
     * @param orderId 취소할 주문 ID
     * @param userId  요청자 ID
     * @return 취소된 주문
     */
    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
                
        orderValidator.validateOrderCancellation(order, userId);
        
        order.cancel();
        return orderRepository.save(order);
    }
    
    /**
     * 현재 시장 가격에 따라 체결 가능한 모든 지정가 주문을 실행합니다.
     *
     * @param symbol       거래 심볼
     * @param currentPrice 현재 시장 가격
     * @return 체결된 주문 수
     */
    @ExecutionTimeLog
    @Transactional
    public int executeOrdersAtPrice(String symbol, BigDecimal currentPrice) {
        List<Order> executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        
        if (executableOrders.isEmpty()) {
            return 0;
        }

        int executedCount = 0;
        for (Order order : executableOrders) {
            try {
                executeOrder(order);
                executedCount++;
            } catch (Exception e) {
                throw new OrderExecutionException(
                    String.format("주문 체결 처리 중 오류 발생 (주문ID: %d, 심볼: %s)", 
                                order.getId(), order.getSymbol()), e);
            }
        }
        
        return executedCount;
    }
    
    /**
     * 사용자 정보 조회
     *
     * @param userId 사용자 ID
     * @return 사용자 객체
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidOrderException("User not found"));
    }
} 