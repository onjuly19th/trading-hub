package com.tradinghub.domain.portfolio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.portfolio.Portfolio;
import com.tradinghub.domain.portfolio.PortfolioService;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("Fetching portfolio: userId={}, username={}", user.getId(), user.getUsername());
        
        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        
        log.debug("Portfolio fetched: userId={}, assetCount={}", 
                user.getId(), portfolio.getAssets() != null ? portfolio.getAssets().size() : 0);
                
        return ResponseEntity.ok(ApiResponse.success(PortfolioResponse.from(portfolio)));
    }
} 