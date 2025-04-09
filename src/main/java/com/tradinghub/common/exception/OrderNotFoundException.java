package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException() {
        super("Order not found", "ORDER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    
    public OrderNotFoundException(Long orderId) {
        super("Order not found: ID=" + orderId, "ORDER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
} 