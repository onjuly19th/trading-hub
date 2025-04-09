package com.tradinghub.common.exception;

import org.springframework.http.HttpStatus;

public class AssetNotFoundException extends BusinessException {
    private static final String ERROR_CODE = "ASSET_NOT_FOUND";
    private static final String MESSAGE = "Asset not found";
    
    public AssetNotFoundException() {
        super(MESSAGE, ERROR_CODE, HttpStatus.NOT_FOUND);
    }
    
    public AssetNotFoundException(String message) {
        super(message, ERROR_CODE, HttpStatus.NOT_FOUND);
    }
} 