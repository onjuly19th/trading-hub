package com.tradinghub.application.usecase.order;

import com.tradinghub.domain.model.order.Order;

public interface CancelOrderUseCase {
    Order execute(Long orderId, Long userId);
}
