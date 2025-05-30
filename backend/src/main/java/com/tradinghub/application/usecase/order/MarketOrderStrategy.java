package com.tradinghub.application.usecase.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.dto.PlaceOrderCommand;
import com.tradinghub.application.port.OrderEventPublisherPort;
import com.tradinghub.application.port.OrderNotificationPort;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.service.OrderValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketOrderStrategy implements OrderStrategy {
    private final OrderValidator orderValidator;
    private final OrderRepository orderRepository;
    private final OrderNotificationPort orderNotificationPort;
    private final OrderEventPublisherPort orderEventPublisherPort;
    
    @Override
    public boolean supports(PlaceOrderCommand command) {
        return command.type() == OrderType.MARKET;
    }

    @Override
    @Transactional
    public Order execute(PlaceOrderCommand command) {
        User user = command.user();
        orderValidator.validateOrderCreation(user, command.side(), command.price(), command.amount());

        Order order = Order.builder()
        .user(user)
        .symbol(command.symbol())
        .side(command.side())
        .type(Order.OrderType.MARKET)
        .price(command.price())
        .amount(command.amount())
        .status(Order.OrderStatus.FILLED)
        .build();

        order.setExecutedPrice(command.price());
        Order savedOrder = orderRepository.save(order);
        
        orderNotificationPort.notifyNewOrder(savedOrder);
        
        orderEventPublisherPort.publishOrderExecuted(savedOrder);
        
        return order;
    }
}
