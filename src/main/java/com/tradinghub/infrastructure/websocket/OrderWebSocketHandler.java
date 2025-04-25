package com.tradinghub.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.order.Order;
import com.tradinghub.domain.order.dto.OrderResponse;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.user.User;

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
        sendOrderNotification(order, "created");
    }
    
    /**
     * 주문 상태가 변경되었을 때 호출
     */
    public void notifyOrderUpdate(Order order) {
        sendOrderNotification(order, "updated");
    }
    
    /**
     * 주문 알림 전송 공통 로직
     */
    private void sendOrderNotification(Order order, String eventType) {
        try {
            OrderResponse orderResponse = OrderResponse.from(order);
            log.debug("Preparing WebSocket message - Order {}: orderId={}, symbol={}, status={}", 
                    eventType, order.getId(), order.getSymbol(), order.getStatus());
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            log.debug("WebSocket entire order message sent: /topic/orders");
            
            // 사용자별 주문 내역 업데이트 (username 사용)
            String destination = "/queue/user/" + order.getUser().getUsername() + "/orders";
            messagingTemplate.convertAndSend(destination, orderResponse);
            
            log.info("Order {} notification sent: orderId={}, destination={}", 
                eventType, order.getId(), destination);
        } catch (Exception e) {
            log.error("Failed to send order {} notification: orderId={}, error={}", 
                    eventType, order.getId(), e.getMessage(), e);
        }
    }
    
    /**
     * 포트폴리오 업데이트가 있을 때 호출
     * 주문 처리, 거래 체결 등으로 인해 포트폴리오가 변경되었을 때 사용
     */
    public void notifyPortfolioUpdate(Portfolio portfolio) {
        sendPortfolioNotification(portfolio.getUser(), portfolio);
    }
    
    /**
     * 사용자 정보를 통해 포트폴리오 업데이트 알림
     */
    public void notifyPortfolioUpdateByUser(User user, Portfolio portfolio) {
        sendPortfolioNotification(user, portfolio);
    }
    
    /**
     * 포트폴리오 알림 전송 공통 로직
     */
    private void sendPortfolioNotification(User user, Portfolio portfolio) {
        try {
            PortfolioResponse portfolioResponse = PortfolioResponse.from(portfolio);
            log.debug("Preparing WebSocket message - Portfolio update: username={}", 
                    user.getUsername());
            
            // 사용자의 포트폴리오 정보 업데이트 (username 사용)
            String destination = "/queue/user/" + user.getUsername() + "/portfolio";
            messagingTemplate.convertAndSend(destination, portfolioResponse);
            
            log.info("Portfolio update notification sent: username={}, destination={}", 
                user.getUsername(), destination);
        } catch (Exception e) {
            log.error("Failed to send portfolio update notification: username={}, error={}", 
                    user.getUsername(), e.getMessage(), e);
        }
    }
} 