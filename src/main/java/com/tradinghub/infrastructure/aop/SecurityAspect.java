package com.tradinghub.infrastructure.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tradinghub.common.exception.UnauthorizedOperationException;
import com.tradinghub.domain.user.User;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class SecurityAspect {

    /**
     * 컨트롤러의 모든 메서드를 대상으로 하는 포인트컷
     */
    @Pointcut("execution(* com.tradinghub.domain.*.controller.*.*(..))")
    private void controllerMethods() {}
    
    /**
     * OrderController의 주문 생성/취소 메서드를 대상으로 하는 포인트컷
     */
    @Pointcut("execution(* com.tradinghub.domain.trading.controller.OrderController.createOrder(..)) || " +
              "execution(* com.tradinghub.domain.trading.controller.OrderController.cancelOrder(..))")
    private void orderOperationMethods() {}
    
    /**
     * 컨트롤러 메서드 실행 전 요청 정보를 로깅합니다.
     */
    @Before("controllerMethods()")
    public void logControllerAccess(JoinPoint joinPoint) {
        // 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "anonymousUser";
        
        // 메서드 정보 로깅
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        
        log.info("Security: User '{}' is accessing {}.{}", username, className, methodName);
    }
    
    /**
     * 주문 관련 작업 실행 후 결과를 로깅합니다.
     */
    @AfterReturning(pointcut = "orderOperationMethods()", returning = "result")
    public void logOrderOperation(JoinPoint joinPoint, Object result) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            String methodName = joinPoint.getSignature().getName();
            
            log.info("Security: User '{}' (ID: {}) successfully performed operation '{}'", 
                    user.getUsername(), user.getId(), methodName);
        }
    }
    
    /**
     * 보안 관련 예외 발생 시 로깅합니다.
     */
    @AfterThrowing(pointcut = "controllerMethods()", throwing = "ex")
    public void logSecurityException(JoinPoint joinPoint, Exception ex) {
        if (ex instanceof UnauthorizedOperationException) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null ? authentication.getName() : "anonymousUser";
            String methodName = joinPoint.getSignature().getName();
            
            log.warn("Security: Access denied for user '{}' attempting to execute '{}'", 
                    username, methodName);
        }
    }
} 