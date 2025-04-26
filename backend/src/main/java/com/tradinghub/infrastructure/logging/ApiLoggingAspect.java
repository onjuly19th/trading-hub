package com.tradinghub.infrastructure.logging;

import java.util.Optional;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 요청 및 응답 로깅을 처리하는 Aspect.
 * Controller, RestController 레벨에서 작동하며 요청 정보, 응답 상태, 실행 시간, 예외 등을 로깅합니다.
 */
@Aspect
@Component
public class ApiLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);

    // Controller 또는 RestController 어노테이션이 붙은 클래스의 모든 public 메서드를 대상으로 함
    @Pointcut("within(@org.springframework.stereotype.Controller *) || within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerPointcut() {}

    /**
     * API 메서드 실행 전후에 로깅을 수행합니다.
     * 요청 정보(메서드, URI, 파라미터), 실행 시간, 응답 상태 등을 로깅합니다.
     */
    @Around("controllerPointcut()")
    public Object logApiExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getCurrentHttpRequest();
        String requestInfo = formatRequestInfo(request);
        String methodSignature = joinPoint.getSignature().toShortString();

        log.info("API Request Start: {} {}", requestInfo, methodSignature);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            int statusCode = extractStatusCode(result);

            log.info("API Request End: {} {} - Status: {}, Time: {}ms",
                     requestInfo, methodSignature, statusCode, executionTime);
            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("API Request Exception: {} {} - Time: {}ms",
                      requestInfo, methodSignature, executionTime);
            throw throwable;
        }
    }

    /**
     * API 메서드 실행 중 예외가 발생했을 때 로깅합니다.
     */
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "ex")
    public void logApiError(JoinPoint joinPoint, Throwable ex) {
        HttpServletRequest request = getCurrentHttpRequest();
        String requestInfo = formatRequestInfo(request);
        String methodSignature = joinPoint.getSignature().toShortString();

        log.error("API Error: {} {} - Error: {}",
                  requestInfo, methodSignature, ex.getMessage(), ex);
    }

    // --- Helper Methods ---

    private HttpServletRequest getCurrentHttpRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);
    }

    private String formatRequestInfo(HttpServletRequest request) {
        if (request == null) {
            return "[N/A]";
        }
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return String.format("[%s %s]", method, uri);
    }

    private int extractStatusCode(Object result) {
        if (result instanceof ResponseEntity) {
             return ((ResponseEntity<?>) result).getStatusCode().value();
        }
        return 200;
    }

    // private String extractUserInfo() {
    //     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    //     if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
    //         Object principal = authentication.getPrincipal();
    //         if (principal instanceof User) {
    //             return "User: " + ((User) principal).getUsername();
    //         } else if (principal instanceof String) {
    //             return "User: " + principal;
    //         }
    //     }
    //     return "";
    // }
}
