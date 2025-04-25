package com.tradinghub.infrastructure.logging;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 메서드 실행 시간 로깅을 담당하는 로거
 * 모든 서비스 메서드의 실행을 로깅합니다.
 */
@Aspect
@Component
@Slf4j
public class MethodLogger {

    /**
     * 모든 서비스 메서드의 실행을 로깅합니다.
     */
    @Around("execution(* com.tradinghub.domain..*.*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        // 메서드 시그니처 정보
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        // 파라미터 정보
        Object[] args = joinPoint.getArgs();
        String params = Arrays.toString(args);
        
        log.info("Executing {}.{}() with params: {}", className, methodName, params);
        
        try {
            // 메서드 실행
            Object result = joinPoint.proceed();
            
            // 실행 시간 및 결과 로깅
            long executionTime = System.currentTimeMillis() - start;
            log.info("Completed {}.{}() in {}ms with result: {}", 
                     className, methodName, executionTime, result);
            
            return result;
        } catch (Exception e) {
            // 예외 발생 시 로깅
            long executionTime = System.currentTimeMillis() - start;
            log.error("Exception in {}.{}() after {}ms - {} : {}", 
                      className, methodName, executionTime, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }
} 