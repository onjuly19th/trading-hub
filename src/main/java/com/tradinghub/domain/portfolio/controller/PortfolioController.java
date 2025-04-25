package com.tradinghub.domain.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.user.User;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 관련 요청을 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    /**
     * 사용자의 포트폴리오를 조회합니다.
     * 로깅은 PortfolioLoggingAspect에서 처리합니다.
     * 
     * @param authentication 인증 정보
     * @return 포트폴리오 정보를 담은 응답
     */
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        return ResponseEntity.ok(PortfolioResponse.from(portfolio));
    }
} 