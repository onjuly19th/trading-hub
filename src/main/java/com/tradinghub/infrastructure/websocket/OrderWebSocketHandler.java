package com.tradinghub.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.trading.Order;
import com.tradinghub.domain.trading.Trade;
import com.tradinghub.domain.trading.dto.OrderResponse;
import com.tradinghub.domain.trading.dto.TradeResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 새로운 주문이 생성되었을 때 호출
     */
    public void notifyNewOrder(Order order) {
        try {
            OrderResponse orderResponse = new OrderResponse(order);
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            
            // 사용자별 주문 내역 업데이트
            messagingTemplate.convertAndSend("/queue/user/" + order.getUser().getId() + "/orders", orderResponse);
            
            log.info("WebSocket: 새 주문 알림 전송 완료 - 주문ID: {}, 사용자ID: {}", order.getId(), order.getUser().getId());
        } catch (Exception e) {
            log.error("WebSocket: 주문 알림 전송 실패", e);
        }
    }
    
    /**
     * 주문 상태가 변경되었을 때 호출
     */
    public void notifyOrderUpdate(Order order) {
        try {
            OrderResponse orderResponse = new OrderResponse(order);
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            
            // 사용자별 주문 내역 업데이트
            messagingTemplate.convertAndSend("/queue/user/" + order.getUser().getId() + "/orders", orderResponse);
            
            log.info("WebSocket: 주문 업데이트 알림 전송 완료 - 주문ID: {}, 사용자ID: {}", order.getId(), order.getUser().getId());
        } catch (Exception e) {
            log.error("WebSocket: 주문 업데이트 알림 전송 실패", e);
        }
    }
    
    /**
     * 새로운 거래가 체결되었을 때 호출
     */
    public void notifyNewTrade(Trade trade) {
        try {
            TradeResponse tradeResponse = TradeResponse.from(trade);
            
            // 전체 거래 내역 업데이트
            messagingTemplate.convertAndSend("/topic/trades", tradeResponse);
            
            // 사용자별 거래 내역 업데이트 (Order가 있는 경우)
            if (trade.getOrder() != null && trade.getOrder().getUser() != null) {
                messagingTemplate.convertAndSend("/queue/user/" + trade.getOrder().getUser().getId() + "/trades", tradeResponse);
            }
            
            // 포트폴리오 소유자에게도 알림
            if (trade.getPortfolio() != null) {
                messagingTemplate.convertAndSend("/queue/user/" + trade.getPortfolio().getUser().getId() + "/trades", tradeResponse);
            }
            
            log.info("WebSocket: 새 거래 알림 전송 완료 - 거래ID: {}", trade.getId());
        } catch (Exception e) {
            log.error("WebSocket: 거래 알림 전송 실패", e);
        }
    }
} 