package com.tradinghub.domain.portfolio;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.trading.Trade;
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

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Trade>>> getTradeHistory(
            @CurrentUser User user,
            @RequestParam(required = false) String symbol) {
        log.info("Fetching trade history - User: {}, Symbol: {}", user.getId(), symbol);
        return ResponseEntity.ok(ApiResponse.success(portfolioService.getTradeHistory(user.getId())));
    }
} 