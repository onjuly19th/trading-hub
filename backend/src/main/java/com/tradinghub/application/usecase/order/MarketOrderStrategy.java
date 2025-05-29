package com.tradinghub.application.usecase.order;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.dto.PlaceOrderCommand;
import com.tradinghub.application.event.OrderExecutedEvent;
import com.tradinghub.application.service.order.OrderValidator;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.order.OrderRepository;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.websocket.OrderWebSocketHandler;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketOrderStrategy implements OrderStrategy {
    private final OrderValidator orderValidator;
    private final OrderRepository orderRepository;
    private final OrderWebSocketHandler webSocketHandler; // TODO: 인터페이스 계층으로의 의존성 제거
    private final ApplicationEventPublisher eventPublisher;
    
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
        webSocketHandler.notifyNewOrder(savedOrder);
        
        OrderExecutedEvent event = new OrderExecutedEvent(order);
        eventPublisher.publishEvent(event); // TODO: 이벤트 퍼블리싱 분리
        
        return order;
    }
}
