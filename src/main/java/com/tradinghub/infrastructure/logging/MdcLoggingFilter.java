package com.tradinghub.infrastructure.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * 웹 요청 처리 시 MDC(Mapped Diagnostic Context)에 컨텍스트 정보를 설정하는 필터.
 * 각 요청마다 고유한 requestId를 생성하고, 인증된 사용자 정보(userId)를 MDC에 추가합니다.
 * 이 정보는 Logback 등 로깅 프레임워크 설정에서 사용하여 모든 로그 라인에 포함될 수 있습니다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 가능한 한 가장 먼저 실행되도록 설정
public class MdcLoggingFilter implements Filter {

    // MDC에서 사용할 키 정의
    private static final String MDC_KEY_REQUEST_ID = "requestId";
    private static final String MDC_KEY_USER_ID = "userId";
    private static final String ANONYMOUS_USER = "anonymous";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 1. 요청 ID 생성 및 설정
        String requestId = UUID.randomUUID().toString().substring(0, 8); // 8자리로 축약
        MDC.put(MDC_KEY_REQUEST_ID, requestId);

        // 2. 사용자 ID 설정 (인증 정보가 있는 경우)
        String userId = extractUserIdFromSecurityContext();
        MDC.put(MDC_KEY_USER_ID, userId);

        try {
            // 다음 필터 또는 서블릿으로 요청 전달
            chain.doFilter(request, response);
        } finally {
            // 3. 중요: 요청 처리가 끝나면 반드시 MDC에서 정보 제거
            //      그렇지 않으면 스레드 풀 환경에서 다른 요청에 잘못된 정보가 로깅될 수 있음
            MDC.remove(MDC_KEY_REQUEST_ID);
            MDC.remove(MDC_KEY_USER_ID);
            // 필요하다면 MDC.clear(); 를 사용하여 모든 컨텍스트 제거
        }
    }

    /**
     * Spring Security 컨텍스트에서 사용자 ID (또는 식별자)를 추출합니다.
     * 인증되지 않은 경우 "anonymous"를 반환합니다.
     */
    private String extractUserIdFromSecurityContext() {
        // SecurityContextHolder에서 SecurityContext를 가져옵니다.
        SecurityContext securityContext = SecurityContextHolder.getContext();
        // SecurityContext에서 Authentication 객체를 가져옵니다.
        Authentication authentication = securityContext.getAuthentication();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Authentication 객체에서 Principal(주체)을 가져옵니다.
            Object principal = authentication.getPrincipal();

            // Principal의 타입에 따라 사용자 식별자를 추출합니다.
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                // Spring Security의 UserDetails 인터페이스를 구현한 경우 username 반환
                return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                // Principal이 단순 문자열인 경우 (예: 토큰 기반 인증의 subject)
                return (String) principal;
            }
            // 필요에 따라 다른 Principal 타입 처리 (예: Custom User 객체)
            // else if (principal instanceof com.tradinghub.domain.user.User) {
            //     return String.valueOf(((com.tradinghub.domain.user.User) principal).getId());
            // }

            // 위 조건에 해당하지 않으면 Principal의 toString() 사용 (최후의 수단)
            return principal.toString();
        }

        // 인증 정보가 없거나 anonymous 사용자인 경우
        return ANONYMOUS_USER;
    }

    // init() 및 destroy() 메서드는 기본 구현을 사용하거나 필요시 오버라이드
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 필터 초기화 로직 (필요한 경우)
    }

    @Override
    public void destroy() {
        // 필터 소멸 로직 (필요한 경우)
    }
}
