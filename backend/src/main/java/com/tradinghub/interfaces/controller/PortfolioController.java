package com.tradinghub.interfaces.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tradinghub.application.usecase.portfolio.GetPortfolioUseCase;
import com.tradinghub.domain.model.portfolio.Portfolio;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.dto.portfolio.PortfolioResponse;

import lombok.RequiredArgsConstructor;

/**
 * 포트폴리오 관련 요청을 처리하는 컨트롤러
 */
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final GetPortfolioUseCase getPortfolioUseCase;

    /**
     * 사용자의 포트폴리오를 조회합니다.
     * 로깅은 PortfolioLoggingAspect에서 처리합니다.
     * 
     * @param authentication 인증 정보
     * @return 포트폴리오 정보를 담은 응답
     */
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(@AuthenticationPrincipal User user) {
        Portfolio portfolio = getPortfolioUseCase.execute(user.getId());
        return ResponseEntity.ok(PortfolioResponse.from(portfolio));
    }
} 