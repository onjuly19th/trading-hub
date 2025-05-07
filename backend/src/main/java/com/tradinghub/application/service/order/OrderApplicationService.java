package com.tradinghub.application.service.order;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.event.OrderExecutedEvent;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.infrastructure.logging.ExecutionTimeLog;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 관련 애플리케이션 로직을 처리하는 서비스
 * 
 * 이 서비스는 다음과 같은 역할을 담당합니다:
 * 1. 도메인 서비스 조율
 * 2. 이벤트 발행
 * 3. 웹소켓 알림
 * 4. 트랜잭션 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderApplicationService {
    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;
    private final OrderWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * 시장가 주문을 생성하고 즉시 체결합니다.
     *
     * @param userId 주문 생성 사용자 ID
     * @param symbol 거래 심볼
     * @param side   매수/매도 구분
     * @param price  주문 가격
     * @param amount 주문 수량
     * @return 생성된 주문 객체
     */
    @ExecutionTimeLog
    @Transactional
    public Order createMarketOrder(Long userId, String symbol, Order.OrderSide side,
                                  BigDecimal price, BigDecimal amount) {
        // 도메인 로직 위임
        Order order = orderCommandService.createMarketOrder(userId, symbol, side, price, amount);
        
        // 웹소켓 알림은 동기적으로 처리 (UI 즉시 갱신)
        webSocketHandler.notifyNewOrder(order);
        
        // 포트폴리오 업데이트 및 알림 발송은 이벤트를 통해 비동기적으로 처리
        publishOrderExecutedEvent(order);
        
        return order;
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
    @ExecutionTimeLog
    @Transactional
    public Order createLimitOrder(Long userId, String symbol, Order.OrderSide side,
                                 BigDecimal price, BigDecimal amount) {
        Order order = orderCommandService.createLimitOrder(userId, symbol, side, price, amount);
        webSocketHandler.notifyNewOrder(order);
        return order;
    }
    
    /**
     * 주문을 취소합니다.
     *
     * @param orderId 취소할 주문 ID
     * @param userId  요청자 ID
     */
    @ExecutionTimeLog
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderCommandService.cancelOrder(orderId, userId);
        webSocketHandler.notifyOrderUpdate(order);
    }
    
    /**
     * 개별 주문 체결 처리
     *
     * @param order 체결할 주문
     */
    @ExecutionTimeLog
    @Transactional
    public void executeOrder(Order order) {
        log.debug("Order execution started: orderId={}, symbol={}", 
                 order.getId(), order.getSymbol());
                 
        // 주문 상태 변경 - 핵심 비즈니스 로직 (동기적 처리)
        Order executedOrder = orderCommandService.executeOrder(order);
        
        // 웹소켓 알림은 동기적으로 처리 (UI 즉시 갱신)
        webSocketHandler.notifyOrderUpdate(executedOrder);
        
        // 포트폴리오 업데이트 및 알림 발송은 이벤트를 통해 비동기적으로 처리
        publishOrderExecutedEvent(executedOrder);
        
        log.debug("Order execution completed and async event published: orderId={}", executedOrder.getId());
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
        return orderCommandService.executeOrdersAtPrice(symbol, currentPrice);
    }
    
    /**
     * 주문 실행 이벤트 발행
     * 이 이벤트는 비동기적으로 처리될 포트폴리오 업데이트 및 알림 발송 등의 후속 작업을 트리거합니다.
     */
    private void publishOrderExecutedEvent(Order order) {
        OrderExecutedEvent event = new OrderExecutedEvent(order);
        log.debug("Order execution event published: orderId={}, symbol={}", 
                 event.getOrderId(), event.getSymbol());
        eventPublisher.publishEvent(event);
    }
    
    // 조회 메서드 - 단순 위임
    
    /**
     * 사용자의 모든 주문을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 주문 목록
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(Long userId) {
        return orderQueryService.getOrdersByUserId(userId);
    }
    
    /**
     * 사용자의 완료된 주문을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 완료된 주문 목록
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getCompletedOrdersByUserId(Long userId) {
        return orderQueryService.getCompletedOrdersByUserId(userId);
    }
    
    /**
     * 사용자 ID와 심볼로 주문을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param symbol 거래 심볼
     * @return 주문 목록
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserIdAndSymbol(Long userId, String symbol) {
        return orderQueryService.getOrdersByUserIdAndSymbol(userId, symbol);
    }
    
    /**
     * 사용자의 미체결 주문을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 미체결 주문 목록
     */
    @ExecutionTimeLog
    @Transactional(readOnly = true)
    public List<Order> getPendingOrdersByUserId(Long userId) {
        return orderQueryService.getPendingOrdersByUserId(userId);
    }
} 