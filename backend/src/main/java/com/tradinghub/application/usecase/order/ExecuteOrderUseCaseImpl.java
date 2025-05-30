package com.tradinghub.application.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.port.OrderEventPublisherPort;
import com.tradinghub.application.port.OrderNotificationPort;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExecuteOrderUseCaseImpl implements ExecuteOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderNotificationPort orderNotificationPort;
    private final OrderEventPublisherPort orderEventPublisherPort;

    @Override
    @Transactional
    public Order execute(Order order) {
        order.fill();
        Order executedOrder = orderRepository.save(order);
        orderNotificationPort.notifyOrderUpdate(executedOrder);
        
        orderEventPublisherPort.publishOrderExecuted(executedOrder);
        return executedOrder;
    }
}
