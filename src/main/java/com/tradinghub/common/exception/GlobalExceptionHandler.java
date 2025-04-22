package com.tradinghub.common.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BusinessException 처리
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.error("BusinessException occurred: code={}, message={}, status={}, path={}", 
            e.getErrorCode(), e.getMessage(), e.getStatus(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(e.getErrorCode())
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(e.getStatus().value())
                .build();
        
        return ResponseEntity
                .status(e.getStatus())
                .body(errorResponse);
    }

    // Validation 예외 처리 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        String errorMessage = e.getBindingResult().getFieldError() != null 
            ? e.getBindingResult().getFieldError().getDefaultMessage() 
            : "Invalid request parameter";
        
        var fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());
            
        log.error("Validation error: fieldErrors={}, path={}", fieldErrors, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INVALID_INPUT)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();
            
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // Validation 예외 처리
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        
        var fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorResponse.FieldError.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());
        
        log.error("Bind error: fieldErrors={}, path={}", fieldErrors, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INVALID_INPUT)
                .message(errorMessage)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .fieldErrors(fieldErrors)
                .build();
            
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // IllegalArgumentException 예외 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        log.error("Invalid argument: message={}, path={}, timestamp={}", 
            e.getMessage(), request.getRequestURI(), LocalDateTime.now());
            
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INVALID_ARGUMENT)
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.BAD_REQUEST.value())
                .build();
            
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    // 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error occurred: type={}, message={}, path={}", 
            e.getClass().getSimpleName(), e.getMessage(), request.getRequestURI(), e);
            
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ErrorCodes.INTERNAL_SERVER_ERROR)
                .message("An internal server error occurred")
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();
            
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}