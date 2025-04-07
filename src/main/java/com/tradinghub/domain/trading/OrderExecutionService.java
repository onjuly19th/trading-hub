package com.tradinghub.domain.trading;

import java.math.BigDecimal;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.trading.event.OrderExecutedEvent;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutionService {
    private final OrderRepository orderRepository;
    private final OrderWebSocketHandler webSocketHandler;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 현재가에 따라 체결 가능한 지정가 주문들을 확인하고 거래 체결
     * 매수 주문: 현재가가 지정가 이하일 때 체결
     * 매도 주문: 현재가가 지정가 이상일 때 체결
     *
     * @param symbol 거래 심볼 (예: BTC/USDT)
     * @param currentPrice 현재 시장 가격
     */
    @Transactional
    public void checkAndExecuteTrades(String symbol, BigDecimal currentPrice) {
        var executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        executableOrders.forEach(this::executeTrade);
    }

    private void executeTrade(Order order) {
        try {
            // 주문 상태 업데이트
            order.fill();
            order.setExecutedPrice(order.getPrice());
            order = orderRepository.save(order);
            
            // 이벤트 발행 - 포트폴리오 업데이트는 리스너에서 처리
            eventPublisher.publishEvent(new OrderExecutedEvent(order));
            
            // WebSocket 알림 전송
            webSocketHandler.notifyOrderUpdate(order);
            
            // 포트폴리오 업데이트 알림은 이벤트 리스너에서 처리됨
            
            log.info("주문 체결 완료: 주문 ID={}, 심볼={}, 가격={}, 수량={}", 
                order.getId(), order.getSymbol(), order.getPrice(), order.getAmount());
        } catch (Exception e) {
            // 체결 실패 시 주문 상태 원복
            order.setStatus(Order.OrderStatus.PENDING);
            order = orderRepository.save(order);
            
            // 실패 알림 전송
            webSocketHandler.notifyOrderUpdate(order);
            
            log.error("주문 체결 실패: 주문 ID={}", order.getId(), e);
        }
    }
} 