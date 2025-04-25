package com.tradinghub.infrastructure.logging;

import java.time.LocalDateTime;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.tradinghub.common.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 예외 처리 로깅을 담당하는 로거
 * 
 * GlobalExceptionHandler의 예외 처리 메서드 실행 전후에
 * 로깅을 수행하여 관심사를 분리합니다.
 */
@Slf4j
@Aspect
@Component
public class ExceptionLogger {

    /**
     * GlobalExceptionHandler의 모든 예외 처리 메서드를 대상으로 함
     */
    @Pointcut("execution(* com.tradinghub.common.exception.GlobalExceptionHandler.*(..))")
    private void exceptionHandlerMethods() {}
    
    /**
     * 예외 처리 메서드 실행 전에 로깅
     * 각 예외 유형에 맞는 형식으로 로그를 기록합니다.
     */
    @Before("exceptionHandlerMethods()")
    public void logExceptionBefore(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Exception exception = null;
        HttpServletRequest request = null;
        
        // 인자에서 예외와 요청 객체 추출
        for (Object arg : args) {
            if (arg instanceof Exception) {
                exception = (Exception) arg;
            }
            if (arg instanceof HttpServletRequest) {
                request = (HttpServletRequest) arg;
            }
        }
        
        if (exception == null || request == null) {
            return;
        }
        
        String path = request.getRequestURI();
        
        // 예외 유형에 따른 로깅
        if (exception instanceof BusinessException) {
            BusinessException be = (BusinessException) exception;
            log.error("BusinessException occurred: code={}, message={}, status={}, path={}", 
                    be.getErrorCode(), be.getMessage(), be.getStatus(), path);
        } 
        else if (exception instanceof org.springframework.web.bind.MethodArgumentNotValidException) {
            var e = (org.springframework.web.bind.MethodArgumentNotValidException) exception;
            var fieldErrors = e.getBindingResult().getFieldErrors();
            log.error("Validation error: fieldErrors={}, path={}", fieldErrors, path);
        } 
        else if (exception instanceof org.springframework.validation.BindException) {
            var e = (org.springframework.validation.BindException) exception;
            var fieldErrors = e.getBindingResult().getFieldErrors();
            log.error("Bind error: fieldErrors={}, path={}", fieldErrors, path);
        }
        else if (exception instanceof IllegalArgumentException) {
            log.error("Invalid argument: message={}, path={}, timestamp={}", 
                    exception.getMessage(), path, LocalDateTime.now());
        }
        else {
            log.error("Unexpected error occurred: type={}, message={}, path={}", 
                    exception.getClass().getSimpleName(), exception.getMessage(), path, exception);
        }
    }
    
    /**
     * 예외 처리 메서드 실행 후 응답 로깅 (필요한 경우)
     */
    @AfterReturning(pointcut = "exceptionHandlerMethods()", returning = "result")
    public void logExceptionAfter(JoinPoint joinPoint, Object result) {
        // 필요한 경우 응답 로깅 (세부 로깅이 필요하면 활성화)
        /*
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            if (response.getBody() instanceof ErrorResponse) {
                ErrorResponse errorResponse = (ErrorResponse) response.getBody();
                log.debug("Error response: status={}, code={}, message={}", 
                        response.getStatusCode(), errorResponse.getErrorCode(), 
                        errorResponse.getMessage());
            }
        }
        */
    }
}
