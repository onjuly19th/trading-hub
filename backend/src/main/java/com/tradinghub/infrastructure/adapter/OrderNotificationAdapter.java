package com.tradinghub.infrastructure.adapter;

import org.springframework.stereotype.Component;

import com.tradinghub.application.port.OrderNotificationPort;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;

/**
 * 주문 알림 포트의 웹소켓 어댑터 구현체
 */
@Component
@RequiredArgsConstructor
public class OrderNotificationAdapter implements OrderNotificationPort {
    private final OrderWebSocketHandler webSocketHandler;

    @Override
    public void notifyNewOrder(Order order) {
        webSocketHandler.notifyNewOrder(order);
    }

    @Override
    public void notifyOrderUpdate(Order order) {
        webSocketHandler.notifyOrderUpdate(order);
    }

    @Override
    public void notifyPortfolioUpdate(Portfolio portfolio) {
        webSocketHandler.notifyPortfolioUpdate(portfolio);
    }
}
