package com.tradinghub.infrastructure.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class PerformanceAspect {

    // 주문 관련 서비스 메서드 포인트컷
    @Pointcut("execution(* com.tradinghub.domain.trading.OrderService.*(..))")
    private void orderServiceMethods() {}
    
    // 포트폴리오 관련 서비스 메서드 포인트컷
    @Pointcut("execution(* com.tradinghub.domain.portfolio.PortfolioService.*(..))")
    private void portfolioServiceMethods() {}
    
    // 성능 모니터링이 필요한 모든 메서드
    @Pointcut("orderServiceMethods() || portfolioServiceMethods()")
    private void performanceMonitoringMethods() {}
    
    /**
     * 성능 모니터링이 필요한 메서드의 실행 시간을 측정하고 로깅합니다.
     * 특정 임계값을 초과하는 경우 경고 로그를 출력합니다.
     */
    @Around("performanceMonitoringMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        
        try {
            // 메서드 실행
            Object result = joinPoint.proceed();
            
            // 실행 시간 측정
            long executionTime = System.currentTimeMillis() - start;
            
            // 실행 시간에 따른 경고 로깅
            if (executionTime > 500) {
                log.warn("⚠ PERFORMANCE ALERT: {}.{}() took {}ms to execute - investigation required", 
                         className, methodName, executionTime);
            } else if (executionTime > 200) {
                log.info("⚠ PERFORMANCE NOTICE: {}.{}() took {}ms to execute", 
                         className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("⚠ PERFORMANCE EXCEPTION: {}.{}() failed after {}ms", 
                      className, methodName, executionTime);
            throw e;
        }
    }
} 