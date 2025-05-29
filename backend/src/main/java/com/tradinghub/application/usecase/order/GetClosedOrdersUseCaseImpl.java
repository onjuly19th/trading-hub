package com.tradinghub.application.usecase.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetClosedOrdersUseCaseImpl implements GetClosedOrdersUseCase {
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Order> execute(Long userId) {
        return orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, List.of(Order.OrderStatus.FILLED, Order.OrderStatus.CANCELLED));
    }
}
