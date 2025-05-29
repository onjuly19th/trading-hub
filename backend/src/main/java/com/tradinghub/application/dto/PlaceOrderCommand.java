package com.tradinghub.application.dto;

import java.math.BigDecimal;

import com.tradinghub.domain.model.order.Order.OrderSide;
import com.tradinghub.domain.model.order.Order.OrderType;
import com.tradinghub.domain.model.user.User;

public record PlaceOrderCommand(
    User user,
    String symbol,
    OrderType type,
    OrderSide side,
    BigDecimal price,
    BigDecimal amount
) {}
