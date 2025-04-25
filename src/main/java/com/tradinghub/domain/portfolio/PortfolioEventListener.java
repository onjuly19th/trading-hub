package com.tradinghub.domain.portfolio;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.order.dto.OrderExecutionRequest;
import com.tradinghub.domain.order.event.OrderExecutedEvent;
import com.tradinghub.infrastructure.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 관련 이벤트 리스너
 * 주문 체결 등의 이벤트를 수신하여 포트폴리오를 업데이트
 */
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
        OrderExecutionRequest request = OrderExecutionRequest.builder()
            .symbol(event.getSymbol())
            .amount(event.getAmount())
            .price(event.getPrice())
            .side(event.getSide())
            .build();
        
        portfolioService.updatePortfolioForOrder(event.getUserId(), request);
        
        Portfolio portfolio = portfolioService.getPortfolio(event.getUserId());
        webSocketHandler.notifyPortfolioUpdate(portfolio);
    }
} 