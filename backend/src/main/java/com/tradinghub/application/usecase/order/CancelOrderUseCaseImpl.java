package com.tradinghub.application.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.application.service.order.OrderValidator;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancelOrderUseCaseImpl implements CancelOrderUseCase {
    private final OrderRepository orderRepository;
    private final OrderValidator orderValidator;
    private final OrderWebSocketHandler webSocketHandler; // TODO: 인터페이스 계층으로의 의존성 제거

    @Override
    @Transactional
    public Order execute(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        
        orderValidator.validateOrderCancellation(order, userId);
        
        order.cancel();
        
        Order savedOrder = orderRepository.save(order);
        
        webSocketHandler.notifyOrderUpdate(savedOrder);
        return savedOrder;
    }
}
