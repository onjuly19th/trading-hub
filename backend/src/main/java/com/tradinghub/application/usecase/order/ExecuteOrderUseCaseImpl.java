package com.tradinghub.application.usecase.order;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.event.OrderExecutedEvent;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExecuteOrderUseCaseImpl implements ExecuteOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderWebSocketHandler webSocketHandler; // TODO: 인터페이스 계층으로의 의존성 제거
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Order execute(Order order) {
        order.fill();
        Order executedOrder = orderRepository.save(order);
        webSocketHandler.notifyOrderUpdate(executedOrder);
        
        OrderExecutedEvent event = new OrderExecutedEvent(executedOrder);
        eventPublisher.publishEvent(event);
        return executedOrder;
    }
}
