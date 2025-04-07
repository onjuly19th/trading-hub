package com.tradinghub.domain.trading;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.trading.dto.TradeResponse;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.security.CurrentUser;

@Slf4j
@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {
    private final TradeRepository tradeRepository;
    
    /**
     * 로그인한 사용자의 거래 내역 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getUserTrades(@CurrentUser User user) {
        log.info("Fetching trades for user: {}", user.getUsername());
        
        try {
            List<Trade> trades = tradeRepository.findByPortfolioUserIdOrderByExecutedAtDesc(user.getId());
            List<TradeResponse> tradeResponses = trades.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
            
            log.info("Found {} trades for user: {}", tradeResponses.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(tradeResponses));
        } catch (Exception e) {
            log.error("Error fetching trades for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("TRADE_FETCH_ERROR", "거래 내역을 불러오는데 실패했습니다."));
        }
    }
    
    /**
     * 특정 심볼의 거래 내역 조회
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<ApiResponse<List<TradeResponse>>> getTradesBySymbol(
            @CurrentUser User user, 
            @PathVariable String symbol) {
        log.info("Fetching trades for user: {} and symbol: {}", user.getUsername(), symbol);
        
        try {
            // 사용자 ID와 심볼로 필터링된 거래 내역 조회
            List<Trade> trades = tradeRepository.findByPortfolioUserIdAndSymbolOrderByExecutedAtDesc(user.getId(), symbol);
            List<TradeResponse> tradeResponses = trades.stream()
                .map(TradeResponse::from)
                .collect(Collectors.toList());
            
            log.info("Found {} trades for user: {} and symbol: {}", 
                    tradeResponses.size(), user.getUsername(), symbol);
            return ResponseEntity.ok(ApiResponse.success(tradeResponses));
        } catch (Exception e) {
            log.error("Error fetching trades for user: {} and symbol: {}", 
                    user.getUsername(), symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("TRADE_FETCH_ERROR", "거래 내역을 불러오는데 실패했습니다."));
        }
    }
    
    /**
     * 최근 거래가 조회
     */
    @GetMapping("/price")
    public ResponseEntity<ApiResponse<Object>> getLatestPrice(@RequestParam String symbol) {
        log.info("Fetching latest price for symbol: {}", symbol);
        
        try {
            // 여기서는 간단히 더미 응답 반환
            // 실제로는 최근 거래가를 조회하는 로직 구현 필요
            final String symbolFinal = symbol;
            return ResponseEntity.ok(ApiResponse.success(new Object() {
                public final String symbol = symbolFinal;
                public final String price = "현재 더미 데이터 반환";
            }));
        } catch (Exception e) {
            log.error("Error fetching latest price for symbol: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("PRICE_FETCH_ERROR", "최근 거래가를 불러오는데 실패했습니다."));
        }
    }
} 