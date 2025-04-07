package com.tradinghub.domain.portfolio;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.security.CurrentUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(@CurrentUser User user) {
        log.info("Fetching portfolio - User: {}", user.getId());
        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        return ResponseEntity.ok(ApiResponse.success(PortfolioResponse.from(portfolio)));
    }
} 