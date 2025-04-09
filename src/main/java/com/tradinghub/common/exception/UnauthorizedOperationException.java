package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends BusinessException {
    public UnauthorizedOperationException(String operation) {
        super("You are not authorized to perform this operation: " + operation, 
              "UNAUTHORIZED_OPERATION", HttpStatus.FORBIDDEN);
    }
} 