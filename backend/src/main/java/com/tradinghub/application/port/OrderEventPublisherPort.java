package com.tradinghub.application.port;

import com.tradinghub.domain.model.order.Order;

public interface OrderEventPublisherPort {
    void publishOrderExecuted(Order order);
}
