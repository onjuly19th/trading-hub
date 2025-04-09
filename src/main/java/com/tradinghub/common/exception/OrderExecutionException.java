package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class OrderExecutionException extends BusinessException {
    public OrderExecutionException(String message) {
        super(message, "ORDER_EXECUTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    public OrderExecutionException(Long orderId, String reason) {
        super("Order execution failed (orderId=" + orderId + "): " + reason, 
              "ORDER_EXECUTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
    }
} 