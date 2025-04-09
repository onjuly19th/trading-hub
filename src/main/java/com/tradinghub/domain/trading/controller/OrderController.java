package com.tradinghub.domain.trading.controller;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.common.exception.InvalidOrderException;
import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.trading.Order;
import com.tradinghub.domain.trading.OrderService;
import com.tradinghub.domain.trading.dto.OrderRequest;
import com.tradinghub.domain.trading.dto.OrderResponse;
import com.tradinghub.domain.user.User;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;
    
    /**
     * 로그인한 사용자의 주문 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("Fetching user orders: userId={}, username={}", user.getId(), user.getUsername());
        
        List<Order> orders = orderService.getOrdersByUserId(user.getId());
        List<OrderResponse> orderResponses = OrderResponse.fromList(orders);
        
        return ResponseEntity.ok(ApiResponse.success(orderResponses));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("Creating order: userId={}, username={}, orderType={}, symbol={}, side={}, price={}, amount={}", 
                user.getId(), user.getUsername(), request.getType(), request.getSymbol(), 
                request.getSide(), request.getPrice(), request.getAmount());
        
        validateOrderRequest(request);
        
        Order order;
        if (request.getType() == Order.OrderType.MARKET) {
            order = orderService.createMarketOrder(
                    user.getId(), request.getSymbol(), request.getSide(), 
                    request.getPrice(), request.getAmount());
            log.info("Market order created: orderId={}, userId={}", order.getId(), user.getId());
        } else {
            order = orderService.createLimitOrder(
                    user.getId(), request.getSymbol(), request.getSide(), 
                    request.getPrice(), request.getAmount());
            log.info("Limit order created: orderId={}, userId={}", order.getId(), user.getId());
        }
        
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }
    
    /**
     * 주문 취소
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("Cancelling order: orderId={}, userId={}", orderId, user.getId());
        
        orderService.cancelOrder(orderId, user.getId());
        log.info("Order cancelled successfully: orderId={}, userId={}", orderId, user.getId());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<OrderResponse>> executeOrders(
            @RequestParam @NotBlank(message = "Symbol is required") String symbol, 
            @RequestParam @DecimalMin(value = "0.00000001", message = "Price must be greater than 0") BigDecimal price) {
        log.info("Executing batch orders: symbol={}, price={}", symbol, price);
        
        int executedCount = orderService.executeOrdersAtPrice(symbol, price);
        String message = String.format("Executed %d orders", executedCount);
        
        log.info("Batch execution completed: symbol={}, price={}, executedCount={}", symbol, price, executedCount);
        
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.withMessage(message, executedCount)));
    }

    /**
     * 로그인한 사용자의 거래 내역 조회 (완료된 주문)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrderHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("Fetching order history: userId={}, username={}", user.getId(), user.getUsername());
        
        List<Order> completedOrders = orderService.getCompletedOrdersByUserId(user.getId());
        
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromList(completedOrders)));
    }
    
    /**
     * 특정 심볼의 거래 내역 조회
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersBySymbol(
            @PathVariable String symbol, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("Fetching orders by symbol: userId={}, username={}, symbol={}", 
                user.getId(), user.getUsername(), symbol);
        
        List<Order> orders = orderService.getOrdersByUserIdAndSymbol(user.getId(), symbol);
        
        log.info("Found orders by symbol: userId={}, symbol={}, count={}", 
                user.getId(), symbol, orders.size());
                
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.fromList(orders)));
    }
    
    private void validateOrderRequest(OrderRequest request) {
        // Bean Validation은 이미 amount > 0인지 확인하므로 제거
        
        // Bean Validation은 price > 0인지 확인하므로 제거
        
        // LIMIT 주문에 price가 필요한지 확인 (Bean Validation으로는 확인할 수 없음)
        if (request.getType() == Order.OrderType.LIMIT && request.getPrice() == null) {
            throw new InvalidOrderException("Limit order requires a price");
        }
    }
} 