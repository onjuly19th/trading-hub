package com.tradinghub.application.usecase.order;

import com.tradinghub.domain.model.order.Order;

public interface ExecuteOrderUseCase {
    Order execute(Order order);
}
