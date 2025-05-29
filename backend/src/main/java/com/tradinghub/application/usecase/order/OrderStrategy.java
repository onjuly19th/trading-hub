package com.tradinghub.application.usecase.order;

import com.tradinghub.application.dto.PlaceOrderCommand;
import com.tradinghub.domain.model.order.Order;

public interface OrderStrategy {
    boolean supports(PlaceOrderCommand command);
    Order execute(PlaceOrderCommand command);
}
