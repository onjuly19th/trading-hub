package com.tradinghub.domain.portfolio;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.domain.trading.dto.TradeResponse;
import com.tradinghub.domain.portfolio.dto.PortfolioResponse;
import com.tradinghub.domain.trading.dto.TradeRequest;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.security.CurrentUser;

@Slf4j
@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(@CurrentUser User user) {
        log.info("Portfolio requested for user: {}", user.getUsername());
        Portfolio portfolio = portfolioService.getPortfolio(user.getId());
        return ResponseEntity.ok(PortfolioResponse.from(portfolio));
    }

    @PostMapping("/trade")
    public ResponseEntity<TradeResponse> executeTrade(
            @CurrentUser User user,
            @Valid @RequestBody TradeRequest request) {
        log.info("Trade execution requested by user: {}", user.getUsername());
        return ResponseEntity.ok(
            TradeResponse.from(portfolioService.executeTrade(user.getId(), request))
        );
    }

    @GetMapping("/trades")
    public ResponseEntity<List<TradeResponse>> getTradeHistory(@CurrentUser User user) {
        log.info("Trade history requested by user: {}", user.getUsername());
        return ResponseEntity.ok(
            portfolioService.getTradeHistory(user.getId())
                .stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList())
        );
    }
} 