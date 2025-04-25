package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.order.event.OrderExecutedEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * 이벤트 처리에 대한 로깅을 담당하는 로거
 * 이벤트 리스너의 실행 시작, 종료, 실패 등을 로깅합니다.
 */
@Slf4j
@Aspect
@Component
public class EventLogger {
    
    /**
     * EventListener 어노테이션이 붙은 메서드의 실행을 로깅합니다.
     * 이벤트 처리의 시작, 종료, 실패 상태를 기록합니다.
     *
     * @param joinPoint AOP 조인 포인트
     * @return 원본 메서드의 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("@annotation(org.springframework.context.event.EventListener)")
    public Object logEventHandling(ProceedingJoinPoint joinPoint) throws Throwable {
        // 이벤트 객체 추출
        Object[] args = joinPoint.getArgs();
        if (args.length > 0 && args[0] instanceof OrderExecutedEvent) {
            OrderExecutedEvent event = (OrderExecutedEvent) args[0];
            
            // 이벤트 처리 시작 로깅
            log.info("Starting event processing: type={}, orderId={}, userId={}, symbol={}, amount={}, price={}",
                event.getClass().getSimpleName(),
                event.getOrderId(),
                event.getUserId(),
                event.getSymbol(),
                event.getAmount(),
                event.getPrice()
            );
            
            long startTime = System.currentTimeMillis();
            try {
                Object result = joinPoint.proceed();
                
                // 이벤트 처리 완료 로깅
                log.info("Event processing completed: type={}, userId={}, duration={}ms",
                    event.getClass().getSimpleName(),
                    event.getUserId(),
                    System.currentTimeMillis() - startTime
                );
                
                return result;
            } catch (Exception e) {
                // 이벤트 처리 실패 로깅
                log.error("Event processing failed: type={}, userId={}, error={}",
                    event.getClass().getSimpleName(),
                    event.getUserId(),
                    e.getMessage(),
                    e
                );
                throw e;
            }
        }
        return joinPoint.proceed();
    }
}
