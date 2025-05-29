package com.tradinghub.application.usecase.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.application.dto.PlaceOrderCommand;
import com.tradinghub.domain.model.order.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlaceOrderUseCaseImpl implements PlaceOrderUseCase {
    private final List<OrderStrategy> strategies;

    @Override
    @Transactional
    public Order execute(PlaceOrderCommand command) {
        OrderStrategy strategy = strategies
            .stream()
            .filter(s -> s.supports(command))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No strategy found for order type: " + command.type()));

        return strategy.execute(command);
    } 
}
