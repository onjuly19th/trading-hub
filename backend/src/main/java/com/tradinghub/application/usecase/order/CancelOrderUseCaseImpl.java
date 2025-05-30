package com.tradinghub.application.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.application.port.OrderNotificationPort;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.service.OrderValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl implements CancelOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderValidator orderValidator;
    private final OrderNotificationPort orderNotificationPort;

    @Override
    @Transactional
    public Order execute(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        orderValidator.validateOrderCancellation(order, userId);
        
        order.cancel();
        
        Order savedOrder = orderRepository.save(order);
        
        orderNotificationPort.notifyOrderUpdate(savedOrder);
        return savedOrder;
    }
}
