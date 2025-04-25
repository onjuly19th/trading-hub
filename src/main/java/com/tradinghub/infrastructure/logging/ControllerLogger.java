package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tradinghub.domain.user.User;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 컨트롤러에 대한 공통 로깅을 처리하는 로거
 * 요청 처리 시간, 예외 발생 등의 정보를 로깅합니다.
 */
@Slf4j
@Aspect
@Component
public class ControllerLogger {

    /**
     * 모든 컨트롤러 클래스를 대상으로 하는 포인트컷
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller)")
    public void controllerPointcut() {}

    /**
     * 컨트롤러 메서드 실행 전후에 로깅을 수행합니다.
     * 메서드 실행 시작, 종료, 소요 시간을 로깅합니다.
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 원본 메서드의 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("controllerPointcut()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String endpoint = getEndpoint(joinPoint);
        
        // 인증된 사용자 정보 로깅 (있는 경우)
        String userInfo = extractUserInfo(joinPoint);
        
        log.info("Request started: [{}] {} {}", endpoint, className, methodName + userInfo);
        
        long startTime = System.currentTimeMillis();
        try {
            // 메서드 실행
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 응답 상태 코드 추출 (있는 경우)
            int statusCode = extractStatusCode(result);
            
            log.info("Request completed: [{}] {} {} - {} in {} ms", 
                    endpoint, className, methodName, statusCode, executionTime);
            
            return result;
        } catch (Exception e) {
            // 예외는 @AfterThrowing에서 처리하므로 다시 throw
            throw e;
        }
    }
    
    /**
     * 컨트롤러 메서드에서 예외가 발생했을 때 로깅합니다.
     * 
     * @param joinPoint AOP 조인 포인트
     * @param e 발생한 예외
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Exception e) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String endpoint = getEndpoint(joinPoint);
        
        log.error("Exception in [{}] {}.{}: {}", 
                endpoint, className, methodName, e.getMessage(), e);
    }
    
    /**
     * 클래스에 정의된 RequestMapping 정보를 추출합니다.
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 엔드포인트 정보
     */
    private String getEndpoint(JoinPoint joinPoint) {
        try {
            Class<?> clazz = joinPoint.getTarget().getClass();
            RequestMapping classMapping = clazz.getAnnotation(RequestMapping.class);
            
            String base = "";
            if (classMapping != null && classMapping.value().length > 0) {
                base = classMapping.value()[0];
            }
            
            return base;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    /**
     * 인증된 사용자 정보를 추출합니다.
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 사용자 정보 문자열
     */
    private String extractUserInfo(JoinPoint joinPoint) {
        try {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof Authentication) {
                    Authentication auth = (Authentication) arg;
                    if (auth.getPrincipal() instanceof User) {
                        User user = (User) auth.getPrincipal();
                        return String.format(" [user=%s, id=%s]", user.getUsername(), user.getId());
                    }
                    return String.format(" [user=%s]", auth.getName());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract user info", e);
        }
        return "";
    }
    
    /**
     * 응답에서 HTTP 상태 코드를 추출합니다.
     * 
     * @param result 컨트롤러 메서드의 반환값
     * @return HTTP 상태 코드
     */
    private int extractStatusCode(Object result) {
        try {
            if (result instanceof ResponseEntity) {
                return ((ResponseEntity<?>) result).getStatusCode().value();
            }
        } catch (Exception e) {
            log.debug("Failed to extract status code", e);
        }
        return 0; // 알 수 없는 상태 코드
    }
} 