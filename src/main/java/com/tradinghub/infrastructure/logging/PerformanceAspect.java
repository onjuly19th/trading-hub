package com.tradinghub.infrastructure.logging;

import com.tradinghub.domain.order.Order;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit; // 시간 단위 사용

/**
 * 애플리케이션 성능 모니터링 및 실행 시간 로깅 Aspect.
 * 1. @ExecutionTimeLog 어노테이션이 붙은 메서드의 실행 시간을 로깅합니다.
 * 2. 특정 서비스 패키지 내 메서드의 실행 시간을 측정하고 임계값 초과 시 경고를 로깅합니다.
 * 3. 특정 메서드(예: executeOrder)의 예외 발생 시 상세 정보를 로깅합니다.
 */
@Aspect
@Component
public class PerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAspect.class);

    // 임계값 설정 (밀리초)
    private static final long WARN_THRESHOLD_MS = 500;
    private static final long INFO_THRESHOLD_MS = 200;

    // --- 포인트컷 정의 ---

    // 1. @ExecutionTimeLog 어노테이션 타겟
    @Pointcut("@annotation(com.tradinghub.infrastructure.logging.ExecutionTimeLog)")
    public void executionTimeLogAnnotated() {}

    // 2. 특정 서비스 패키지 타겟
    @Pointcut("execution(* com.tradinghub.domain..*Service.*(..)) || execution(* com.tradinghub.domain..*Repository.*(..))") // domain 하위의 Service 또는 Repository
    public void serviceAndRepositoryMethods() {}

    // --- 어드바이스 정의 ---

    /**
     * @ExecutionTimeLog 어노테이션이 붙은 메서드의 실행 시간을 로깅합니다.
     */
    @Around("executionTimeLogAnnotated()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime(); // 더 정밀한 nanoTime 사용
        try {
            Object result = joinPoint.proceed();
            logExecution(joinPoint, System.nanoTime() - startTime); // 시간 로깅 헬퍼 호출
            return result;
        } catch (Throwable throwable) {
            logExecution(joinPoint, System.nanoTime() - startTime, throwable); // 예외 포함 로깅 헬퍼 호출
            throw throwable;
        }
    }

    /**
     * 서비스 및 리포지토리 메서드의 성능을 모니터링하고 임계값을 초과하면 로깅합니다.
     * (@ExecutionTimeLog가 붙은 메서드는 위에서 처리되므로 중복 로깅 방지 필요 - 여기서는 간단히 중복 실행)
     * TODO: 포인트컷 결합 시 중복 실행 방지 로직 추가 가능 (@Around("serviceAndRepositoryMethods() && !executionTimeLogAnnotated()") 등)
     */
    @Around("serviceAndRepositoryMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            logPerformanceThreshold(joinPoint, System.nanoTime() - startTime); // 임계값 로깅 헬퍼 호출
            return result;
        } catch (Throwable throwable) {
            logDetailedErrorOnSpecificMethods(joinPoint, System.nanoTime() - startTime, throwable); // 특정 메서드 오류 로깅
            // 공통 예외 시간 로깅
            logPerformanceThreshold(joinPoint, System.nanoTime() - startTime, throwable);
            throw throwable;
        }
    }


    // --- 로깅 헬퍼 메서드 ---

    /**
     * 실행 시간 정보를 로그에 기록합니다. (nanoTime을 ms로 변환)
     */
    private void logExecution(ProceedingJoinPoint joinPoint, long nanoTime, Throwable throwable) {
        long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(nanoTime);
        String methodSignature = joinPoint.getSignature().toShortString();
        String message = "Execution time for {} : {}ms";

        // ExecutionTimeLog 어노테이션 값 가져오기 (메서드 시그니처에서 찾아야 함)
        // 여기서는 간단히 INFO 레벨로 고정. 필요시 ExecutionTimeLogger 로직 재활용
        if (throwable == null) {
            log.info(message, methodSignature, executionTimeMs);
        } else {
            log.error(message + " (failed)", methodSignature, executionTimeMs, throwable);
        }
    }
     private void logExecution(ProceedingJoinPoint joinPoint, long nanoTime) {
        logExecution(joinPoint, nanoTime, null);
     }


    /**
     * 실행 시간이 임계값을 초과하는지 확인하고 로그를 기록합니다.
     */
    private void logPerformanceThreshold(ProceedingJoinPoint joinPoint, long nanoTime, Throwable throwable) {
        long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(nanoTime);
        String methodSignature = joinPoint.getSignature().toShortString(); // 더 간단하게 변경
        String baseMessage = "{}{} took {}ms";

        if (throwable == null) { // 성공 시
            if (executionTimeMs > WARN_THRESHOLD_MS) {
                log.warn("PERFORMANCE ALERT: " + baseMessage, methodSignature, "", executionTimeMs);
            } else if (executionTimeMs > INFO_THRESHOLD_MS) {
                log.info("PERFORMANCE NOTICE: " + baseMessage, methodSignature, "", executionTimeMs);
            }
            // 임계값 미만은 DEBUG 레벨로 로깅하거나 생략 가능
            // else { log.debug(baseMessage, methodSignature, "", executionTimeMs); }
        } else { // 실패 시
             String failurePostfix = " (failed)";
             if (executionTimeMs > WARN_THRESHOLD_MS) {
                log.warn("PERFORMANCE ALERT: " + baseMessage, methodSignature, failurePostfix, executionTimeMs);
            } else if (executionTimeMs > INFO_THRESHOLD_MS) {
                // 실패는 최소 INFO 레벨로 기록하는 것이 좋음
                log.info("PERFORMANCE NOTICE: " + baseMessage, methodSignature, failurePostfix, executionTimeMs);
            } else {
                 // 임계값 미만이라도 실패는 INFO 레벨로 기록
                 log.info(baseMessage, methodSignature, failurePostfix, executionTimeMs);
            }
        }
    }
     private void logPerformanceThreshold(ProceedingJoinPoint joinPoint, long nanoTime) {
        logPerformanceThreshold(joinPoint, nanoTime, null);
     }


    /**
     * 특정 메서드(예: executeOrder)에서 예외 발생 시 상세 정보를 로깅합니다.
     */
    private void logDetailedErrorOnSpecificMethods(ProceedingJoinPoint joinPoint, long nanoTime, Throwable throwable) {
         long executionTimeMs = TimeUnit.NANOSECONDS.toMillis(nanoTime);
         String className = joinPoint.getSignature().getDeclaringTypeName();
         String methodName = joinPoint.getSignature().getName();

         if (className.equals("com.tradinghub.domain.order.application.OrderApplicationService")
                && methodName.equals("executeOrder")) {
                Object[] args = joinPoint.getArgs();
                if (args.length > 0 && args[0] instanceof Order) {
                    Order order = (Order) args[0];
                    // MDC를 사용하므로 userId는 제거
                    log.error(
                        "Order execution failed - orderId: {}, symbol: {}, type: {}, side: {}, price: {}, amount: {}, execution time: {}ms, error: {}",
                        order.getId(), /* order.getUser().getId(), */ order.getSymbol(),
                        order.getType(), order.getSide(), order.getPrice(),
                        order.getAmount(), executionTimeMs, throwable.getMessage(), throwable // 스택트레이스 포함
                    );
                    // 여기서 throw를 다시 할 필요 없음. 호출한 곳에서 처리됨.
                } else {
                     // 파라미터 타입이 예상과 다를 경우
                     log.error("PERFORMANCE EXCEPTION (executeOrder with unexpected args): {}.{} failed after {}ms - Error: {}",
                               className, methodName, executionTimeMs, throwable.getMessage(), throwable);
                }
         }
         // 다른 특정 메서드에 대한 상세 로깅 추가 가능
    }
}
