package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.order.Order;

import lombok.extern.slf4j.Slf4j;

/**
 * 성능 모니터링을 담당하는 로거
 * 주문, 포트폴리오, 사용자 서비스 메서드의 실행 시간을 측정하고 로깅합니다.
 */
@Aspect
@Component
@Slf4j
public class PerformanceMonitor {

    // 주문 관련 서비스 메서드 포인트컷
    @Pointcut("execution(* com.tradinghub.domain.order.application.OrderApplicationService.*(..))")
    private void orderServiceMethods() {}
    
    // 포트폴리오 관련 서비스 메서드 포인트컷
    @Pointcut("execution(* com.tradinghub.domain.portfolio.PortfolioService.*(..))")
    private void portfolioServiceMethods() {}
    
    // 사용자 관련 서비스 메서드 포인트컷 추가
    @Pointcut("execution(* com.tradinghub.domain.user.UserService.*(..))")
    private void userServiceMethods() {}
    
    // 성능 모니터링이 필요한 모든 메서드
    @Pointcut("orderServiceMethods() || portfolioServiceMethods() || userServiceMethods()")
    private void performanceMonitoringMethods() {}
    
    /**
     * 성능 모니터링이 필요한 메서드의 실행 시간을 측정하고 로깅합니다.
     * 특정 임계값을 초과하는 경우 경고 로그를 출력합니다.
     * OrderApplicationService.executeOrder 메서드의 경우 실패 시 상세 로깅을 수행합니다.
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
                log.warn("PERFORMANCE ALERT: {}.{}() took {}ms to execute - investigation required", 
                         className, methodName, executionTime);
            } else if (executionTime > 200) {
                log.info("PERFORMANCE NOTICE: {}.{}() took {}ms to execute", 
                         className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            
            // OrderApplicationService.executeOrder 메서드인 경우 상세 로깅 추가
            if (className.equals("com.tradinghub.domain.order.application.OrderApplicationService") 
                && methodName.equals("executeOrder")) {
                Object[] args = joinPoint.getArgs();
                if (args.length > 0 && args[0] instanceof Order) {
                    Order order = (Order) args[0];
                    log.error(
                        "Order execution failed - orderId: {}, userId: {}, symbol: {}, type: {}, side: {}, price: {}, amount: {}, execution time: {}ms, error: {}",
                        order.getId(), order.getUser().getId(), order.getSymbol(),
                        order.getType(), order.getSide(), order.getPrice(),
                        order.getAmount(), executionTime, e.getMessage()
                    );
                    throw e;
                }
            }
            
            log.error("PERFORMANCE EXCEPTION: {}.{}() failed after {}ms", 
                      className, methodName, executionTime);
            throw e;
        }
    }
} 