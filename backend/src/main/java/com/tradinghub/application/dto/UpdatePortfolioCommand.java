package com.tradinghub.application.dto;

import java.math.BigDecimal;

import com.tradinghub.domain.model.order.Order.OrderSide;

public record UpdatePortfolioCommand(
    String symbol,
    BigDecimal amount,
    BigDecimal price,
    OrderSide side
) {}
