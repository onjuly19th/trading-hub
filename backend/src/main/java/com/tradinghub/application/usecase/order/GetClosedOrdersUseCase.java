package com.tradinghub.application.usecase.order;

import java.util.List;

import com.tradinghub.domain.model.order.Order;

public interface GetClosedOrdersUseCase {
    List<Order> execute(Long userId);
}
