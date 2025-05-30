package com.tradinghub.infrastructure.adapter;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.tradinghub.application.port.OrderEventPublisherPort;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.application.event.OrderExecutedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderEventPublisherAdapter implements OrderEventPublisherPort {
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publishOrderExecuted(Order order) {
        eventPublisher.publishEvent(new OrderExecutedEvent(order));
    }
}
