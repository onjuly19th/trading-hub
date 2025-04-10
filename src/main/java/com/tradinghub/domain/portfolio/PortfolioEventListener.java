package com.tradinghub.domain.portfolio;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.trading.event.OrderExecutedEvent;
import com.tradinghub.domain.trading.dto.OrderExecutionRequest;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 관련 이벤트 리스너
 * 주문 체결 등의 이벤트를 수신하여 포트폴리오를 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioEventListener {

    private final PortfolioService portfolioService;
    private final OrderWebSocketHandler webSocketHandler;
    
    /**
     * 주문 체결 이벤트 처리
     * 주문이 체결되면 해당 사용자의 포트폴리오를 업데이트하고 웹소켓으로 알림
     */
    @EventListener
    @Transactional
    public void handleOrderExecuted(OrderExecutedEvent event) {
        log.info("Order execution event received: orderId={}, userId={}, symbol={}, amount={}, price={}",
            event.getOrderId(), event.getUserId(), event.getSymbol(), event.getAmount(), event.getPrice());
        
        // 이벤트 정보로 포트폴리오 업데이트 요청 생성
        OrderExecutionRequest request = OrderExecutionRequest.builder()
            .symbol(event.getSymbol())
            .amount(event.getAmount())
            .price(event.getPrice())
            .side(event.getSide())
            .build();
        
        // 포트폴리오 업데이트 실행
        portfolioService.updatePortfolioForOrder(event.getUserId(), request);
        
        // 포트폴리오 업데이트 후 웹소켓으로 알림 전송
        Portfolio portfolio = portfolioService.getPortfolio(event.getUserId());
        webSocketHandler.notifyPortfolioUpdate(portfolio);
        
        log.info("Portfolio update and notification completed for order execution: userId={}", event.getUserId());
    }
} 