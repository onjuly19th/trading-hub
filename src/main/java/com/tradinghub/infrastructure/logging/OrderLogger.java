package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.order.dto.OrderResponse;
import com.tradinghub.domain.user.User;

import lombok.extern.slf4j.Slf4j;

/**
 * 주문 관련 작업을 로깅하는 로거
 * 주문 생성/취소 메서드의 실행 결과를 로깅합니다.
 */
@Aspect
@Component
@Slf4j
public class OrderLogger {
    
    /**
     * OrderController의 주문 생성/취소 메서드를 대상으로 하는 포인트컷
     */
    @Pointcut("execution(* com.tradinghub.domain.order.controller.OrderController.createOrder(..)) || " +
              "execution(* com.tradinghub.domain.order.controller.OrderController.cancelOrder(..))")
    private void orderOperationMethods() {}
    
    /**
     * 주문 관련 작업 실행 후 결과를 로깅합니다.
     */
    @AfterReturning(pointcut = "orderOperationMethods()", returning = "result")
    public void logOrderOperation(JoinPoint joinPoint, Object result) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            String methodName = joinPoint.getSignature().getName();
            
            // 응답에서 추가 정보 추출
            if (result instanceof ResponseEntity && methodName.equals("createOrder")) {
                ResponseEntity<?> response = (ResponseEntity<?>) result;
                if (response.getBody() instanceof OrderResponse) {
                    OrderResponse orderResponse = (OrderResponse) response.getBody();
                    log.info("Order: User '{}' (ID: {}) created order - symbol: {}, type: {}, side: {}, amount: {}, timestamp: {}", 
                            user.getUsername(), user.getId(), 
                            orderResponse.getSymbol(),
                            orderResponse.getType(),
                            orderResponse.getSide(),
                            orderResponse.getAmount(),
                            orderResponse.getTimestamp());
                    return;
                }
            }
            
            // 기본 로깅
            log.info("Order: User '{}' (ID: {}) successfully performed operation '{}'", 
                    user.getUsername(), user.getId(), methodName);
        }
    }
}