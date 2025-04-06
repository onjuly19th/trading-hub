package com.tradinghub.domain.trading;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.trading.dto.OrderRequest;
import com.tradinghub.domain.trading.dto.OrderResponse;
import com.tradinghub.domain.trading.dto.TradeResponse;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.security.CurrentUser;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createOrder(@CurrentUser User user, @RequestBody OrderRequest request) {
        log.info("Creating {} order - User: {}, Symbol: {}", 
            request.getType(), user.getId(), request.getSymbol());
        
        validateOrderRequest(request);

        if (request.getType() == Order.OrderType.MARKET) {
            Trade trade = orderService.createMarketOrder(
                user.getId(),
                request.getSymbol(),
                request.getSide(),
                request.getPrice(),
                request.getAmount()
            );
            log.info("Market order executed - Trade ID: {}", trade.getId());
            return ResponseEntity.ok(ApiResponse.success(TradeResponse.from(trade)));
        } else {
            Order order = orderService.createLimitOrder(
                user.getId(),
                request.getSymbol(),
                request.getSide(),
                request.getPrice(),
                request.getAmount()
            );
            log.info("Limit order created - Order ID: {}", order.getId());
            return ResponseEntity.ok(ApiResponse.success(new OrderResponse(order)));
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<OrderResponse>> executeOrders(@RequestBody String symbol, BigDecimal price) {
        log.info("Executing orders - Symbol: {}", symbol);
        
        try {
            int executedCount = orderService.executeOrdersAtPrice(symbol, price);
            String message = executedCount > 0 ? 
                String.format("%d개 주문 체결 완료", executedCount) : 
                "체결 가능한 주문 없음";
                
            log.info("Order execution completed - {}", message);
            return ResponseEntity.ok(ApiResponse.success(new OrderResponse(message, executedCount)));
        } catch (Exception e) {
            log.error("Order execution failed - Symbol: {}", symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ORDER_EXECUTION_FAILED", "주문 체결 중 오류가 발생했습니다"));
        }
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }
        if (request.getType() == null) {
            throw new IllegalArgumentException("Order type is required");
        }
        if (request.getSide() == null) {
            throw new IllegalArgumentException("Order side is required");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Valid amount is required");
        }
        if (request.getType() == Order.OrderType.LIMIT && 
            (request.getPrice() == null || request.getPrice().signum() <= 0)) {
            throw new IllegalArgumentException("Valid price is required for limit orders");
        }
    }
} 