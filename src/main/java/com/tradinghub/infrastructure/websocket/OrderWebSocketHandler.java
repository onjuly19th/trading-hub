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
            log.info("WebSocket 메시지 준비 - 새 주문: {}", orderResponse);
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            log.info("WebSocket 전체 주문 메시지 전송 완료: /topic/orders");
            
            // 사용자별 주문 내역 업데이트 (username 사용)
            String destination = "/queue/user/" + order.getUser().getUsername() + "/orders";
            messagingTemplate.convertAndSend(destination, orderResponse);
            
            log.info("WebSocket: 새 주문 알림 전송 완료 - 주문ID: {}, 사용자: {}, 경로: {}", 
                order.getId(), order.getUser().getUsername(), destination);
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
            log.info("WebSocket 메시지 준비 - 주문 업데이트: {}", orderResponse);
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            log.info("WebSocket 전체 주문 메시지 전송 완료: /topic/orders");
            
            // 사용자별 주문 내역 업데이트 (username 사용)
            String destination = "/queue/user/" + order.getUser().getUsername() + "/orders";
            messagingTemplate.convertAndSend(destination, orderResponse);
            
            log.info("WebSocket: 주문 업데이트 알림 전송 완료 - 주문ID: {}, 사용자: {}, 경로: {}", 
                order.getId(), order.getUser().getUsername(), destination);
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
            log.info("WebSocket 메시지 준비 - 새 거래: {}", tradeResponse);
            
            // 전체 거래 내역 업데이트
            messagingTemplate.convertAndSend("/topic/trades", tradeResponse);
            log.info("WebSocket 전체 거래 메시지 전송 완료: /topic/trades");
            
            // 사용자별 거래 내역 업데이트 (Order가 있는 경우) (username 사용)
            if (trade.getOrder() != null && trade.getOrder().getUser() != null) {
                String destination = "/queue/user/" + trade.getOrder().getUser().getUsername() + "/trades";
                messagingTemplate.convertAndSend(destination, tradeResponse);
                log.info("WebSocket: 사용자별 거래 알림 전송 완료 - 거래ID: {}, 사용자: {}, 경로: {}", 
                    trade.getId(), trade.getOrder().getUser().getUsername(), destination);
            }
            
            // 포트폴리오 소유자에게도 알림 (username 사용)
            if (trade.getPortfolio() != null) {
                String destination = "/queue/user/" + trade.getPortfolio().getUser().getUsername() + "/trades";
                messagingTemplate.convertAndSend(destination, tradeResponse);
                log.info("WebSocket: 포트폴리오 소유자 거래 알림 전송 완료 - 거래ID: {}, 사용자: {}, 경로: {}", 
                    trade.getId(), trade.getPortfolio().getUser().getUsername(), destination);
            }
            
            log.info("WebSocket: 새 거래 알림 전송 완료 - 거래ID: {}", trade.getId());
        } catch (Exception e) {
            log.error("WebSocket: 거래 알림 전송 실패", e);
        }
    }
} 