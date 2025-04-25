package com.tradinghub.infrastructure.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.user.User;

import lombok.extern.slf4j.Slf4j;

/**
 * 포트폴리오 관련 작업의 로깅을 담당하는 로거
 * 컨트롤러에서 로깅 코드를 분리하여 관심사 분리 및 코드 중복 방지
 */
@Slf4j
@Aspect
@Component
public class PortfolioLogger {

    /**
     * 포트폴리오 조회 작업에 대한 로깅을 처리합니다.
     * 요청 전 사용자 정보를 로그로 남기고, 응답 후 포트폴리오 정보를 로그로 남깁니다.
     * 
     * @param joinPoint AOP 조인 포인트
     * @return 원본 메서드의 실행 결과
     * @throws Throwable 메서드 실행 중 발생할 수 있는 예외
     */
    @Around("execution(* com.tradinghub.domain.portfolio.controller.PortfolioController.getPortfolio(..))")
    public Object logPortfolioFetch(ProceedingJoinPoint joinPoint) throws Throwable {
        // 메서드 실행 전 로깅
        Authentication authentication = (Authentication) joinPoint.getArgs()[0];
        User user = (User) authentication.getPrincipal();
        log.info("Fetching portfolio: userId={}, username={}", user.getId(), user.getUsername());

        // 메서드 실행
        Object result = joinPoint.proceed();

        // 메서드 실행 후 로깅
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> response = (ResponseEntity<?>) result;
            if (response.getBody() instanceof PortfolioResponse) {
                PortfolioResponse portfolioResponse = (PortfolioResponse) response.getBody();
                log.debug("Portfolio fetched: userId={}, assetCount={}, timestamp={}", 
                        user.getId(), 
                        portfolioResponse.getAssets() != null ? portfolioResponse.getAssets().size() : 0,
                        portfolioResponse.getTimestamp());
            }
        }

        return result;
    }
} 