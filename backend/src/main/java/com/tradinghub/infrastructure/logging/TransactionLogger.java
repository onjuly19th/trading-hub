package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * 트랜잭션 실행 로깅을 담당하는 로거
 * 트랜잭션 시작, 커밋, 롤백 시점에 대한 로깅을 수행합니다.
 */
@Aspect
@Component
@Slf4j
public class TransactionLogger {

    /**
     * @Transactional 어노테이션이 적용된 모든 메서드
     */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    private void transactionalMethods() {}
    
    /**
     * 트랜잭션이 적용된 메서드의 실행을 로깅합니다.
     */
    @Around("transactionalMethods() && @annotation(transactional)")
    public Object logTransactionExecution(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        boolean readOnly = transactional.readOnly();
        String isolationLevel = transactional.isolation().name();
        
        log.info("Transaction STARTED: {}.{}() [isolation={}, readOnly={}]", 
                className, methodName, isolationLevel, readOnly);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Transaction COMMITTED: {}.{}() completed in {}ms", 
                    className, methodName, executionTime);
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Transaction ROLLED BACK: {}.{}() after {}ms due to: {}", 
                    className, methodName, executionTime, e.getMessage());
            
            throw e;
        }
    }
} 