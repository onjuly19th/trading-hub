package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 메서드 실행 시간 측정을 위한 로거
 */
@Aspect
@Component
public class ExecutionTimeLogger {
    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeLogger.class);

    /**
     * @ExecutionTimeLog 어노테이션이 적용된 메서드의 실행 시간을 측정
     */
    @Around("@annotation(executionTimeLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, ExecutionTimeLog executionTimeLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        String level = executionTimeLog.level().toUpperCase();
        String message = executionTimeLog.message();
        String methodName = joinPoint.getSignature().getName();

        switch (level) {
            case "TRACE":
                log.trace(message, methodName, executionTime);
                break;
            case "DEBUG":
                log.debug(message, methodName, executionTime);
                break;
            case "INFO":
                log.info(message, methodName, executionTime);
                break;
            case "WARN":
                log.warn(message, methodName, executionTime);
                break;
            case "ERROR":
                log.error(message, methodName, executionTime);
                break;
            default:
                log.info(message, methodName, executionTime);
        }

        return result;
    }
} 