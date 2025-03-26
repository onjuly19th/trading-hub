package com.tradinghub.domain.trading.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import com.tradinghub.domain.trading.Trade.TradeType;

@Getter
@Setter
public class TradeRequest {
    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00000001", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Trade type is required")
    private TradeType type;

    public boolean isBuy() {
        return TradeType.BUY.equals(type);
    }
} 