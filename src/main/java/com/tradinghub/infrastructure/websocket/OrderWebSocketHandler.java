package com.tradinghub.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.trading.Order;
import com.tradinghub.domain.trading.dto.OrderResponse;
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
        try {
            OrderResponse orderResponse = new OrderResponse(order);
            log.info("WebSocket Message Ready - New Order: {}", orderResponse);
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            log.info("WebSocket Entire Order Message Sent: /topic/orders");
            
            // 사용자별 주문 내역 업데이트 (username 사용)
            String destination = "/queue/user/" + order.getUser().getUsername() + "/orders";
            messagingTemplate.convertAndSend(destination, orderResponse);
            
            log.info("WebSocket New Order Message Sent: {}, {}, {}", 
                order.getId(), order.getUser().getUsername(), destination);
        } catch (Exception e) {
            log.error("WebSocket: Order Notification Failed", e);
        }
    }
    
    /**
     * 주문 상태가 변경되었을 때 호출
     */
    public void notifyOrderUpdate(Order order) {
        try {
            OrderResponse orderResponse = new OrderResponse(order);
            log.info("WebSocket Message Ready - Order Update: {}", orderResponse);
            
            // 전체 주문 내역 업데이트
            messagingTemplate.convertAndSend("/topic/orders", orderResponse);
            log.info("WebSocket Entire Order Message Sent: /topic/orders");
            
            // 사용자별 주문 내역 업데이트 (username 사용)
            String destination = "/queue/user/" + order.getUser().getUsername() + "/orders";
            messagingTemplate.convertAndSend(destination, orderResponse);
            
            log.info("WebSocket: Order Update Message Sent: {}, {}, {}", 
                order.getId(), order.getUser().getUsername(), destination);
        } catch (Exception e) {
            log.error("WebSocket: Order Update Notification Failed", e);
        }
    }
    
    /**
     * 포트폴리오 업데이트가 있을 때 호출
     * 주문 처리, 거래 체결 등으로 인해 포트폴리오가 변경되었을 때 사용
     */
    public void notifyPortfolioUpdate(Portfolio portfolio) {
        try {
            PortfolioResponse portfolioResponse = PortfolioResponse.from(portfolio);
            log.info("WebSocket Message Ready - Portfolio Update: {}", portfolio.getUser().getUsername());
            
            // 사용자의 포트폴리오 정보 업데이트 (username 사용)
            String destination = "/queue/user/" + portfolio.getUser().getUsername() + "/portfolio";
            messagingTemplate.convertAndSend(destination, ApiResponse.success(portfolioResponse));
            
            log.info("WebSocket: Portfolio Update Notification Sent: {}, {}", 
                portfolio.getUser().getUsername(), destination);
        } catch (Exception e) {
            log.error("WebSocket: Portfolio Update Notification Failed", e);
        }
    }
    
    /**
     * 사용자 정보를 통해 포트폴리오 업데이트 알림
     */
    public void notifyPortfolioUpdateByUser(User user, Portfolio portfolio) {
        try {
            PortfolioResponse portfolioResponse = PortfolioResponse.from(portfolio);
            log.info("WebSocket Message Ready - Portfolio Update: {}", user.getUsername());
            
            // 사용자의 포트폴리오 정보 업데이트 (username 사용)
            String destination = "/queue/user/" + user.getUsername() + "/portfolio";
            messagingTemplate.convertAndSend(destination, ApiResponse.success(portfolioResponse));
            
            log.info("WebSocket: Portfolio Update Notification Sent: {}, {}", 
                user.getUsername(), destination);
        } catch (Exception e) {
            log.error("WebSocket: Portfolio Update Notification Failed", e);
        }
    }
} 