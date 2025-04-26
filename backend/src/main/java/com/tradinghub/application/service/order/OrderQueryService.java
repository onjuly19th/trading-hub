package com.tradinghub.application.service.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.repository.OrderRepository;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;

import lombok.RequiredArgsConstructor;

/**
 * 주문 조회를 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;
    
    /**
     * 사용자의 모든 주문을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 주문 목록 (생성일시 내림차순)
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    /**
     * 사용자 ID로 완료된 주문 조회 (체결 또는 취소)
     *
     * @param userId 사용자 ID
     * @return 완료된 주문 목록 (생성일시 내림차순)
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getCompletedOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of(Order.OrderStatus.FILLED, Order.OrderStatus.CANCELLED));
    }
    
    /**
     * 사용자 ID와 심볼로 주문 조회
     *
     * @param userId 사용자 ID
     * @param symbol 거래 심볼
     * @return 주문 목록 (생성일시 내림차순)
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserIdAndSymbol(Long userId, String symbol) {
        return orderRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(userId, symbol);
    }
    
    /**
     * 주문 ID로 주문 조회
     *
     * @param orderId 주문 ID
     * @return 주문 객체
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
    }
} 