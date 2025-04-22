package com.tradinghub.domain.trading.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tradinghub.common.exception.order.InvalidOrderException;
import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.trading.Order;
import com.tradinghub.domain.trading.OrderService;
import com.tradinghub.domain.trading.dto.OrderRequest;
import com.tradinghub.domain.trading.dto.OrderResponse;
import com.tradinghub.domain.user.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        List<Order> orders = orderService.getOrdersByUserId(user.getId());
        List<OrderResponse> orderResponses = OrderResponse.fromList(orders);
        
        return ResponseEntity.ok(ApiResponse.success(orderResponses));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        
        validateOrderRequest(request);
        
        Order order;
        if (request.getType() == Order.OrderType.MARKET) {
            order = orderService.createMarketOrder(
                    user.getId(), request.getSymbol(), request.getSide(), 
                    request.getPrice(), request.getAmount());
        } else {
            order = orderService.createLimitOrder(
                    user.getId(), request.getSymbol(), request.getSide(), 
                    request.getPrice(), request.getAmount());
        }
        
        log.info("Order created: orderId={}, type={}, userId={}, symbol={}, side={}, amount={}", 
                order.getId(), request.getType(), user.getId(), request.getSymbol(), 
                request.getSide(), request.getAmount());
        
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }
    
    /**
     * 주문 취소
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable Long orderId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        orderService.cancelOrder(orderId, user.getId());
        
        log.info("Order cancelled: orderId={}, userId={}", orderId, user.getId());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<OrderResponse>> executeOrders(
            @RequestParam @NotBlank(message = "Symbol is required") String symbol, 
            @RequestParam @DecimalMin(value = "0.00000001", message = "Price must be greater than 0") BigDecimal price) {
        int executedCount = orderService.executeOrdersAtPrice(symbol, price);
        String message = String.format("Executed %d orders", executedCount);
        
        log.info("Batch execution completed: symbol={}, price={}, executedCount={}", 
                symbol, price, executedCount);
        
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.withMessage(message, executedCount)));
    }

    /**
     * 로그인한 사용자의 거래 내역 조회 (완료된 주문)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrderHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
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
        List<Order> orders = orderService.getOrdersByUserIdAndSymbol(user.getId(), symbol);
        
        log.info("Orders found for symbol: symbol={}, userId={}, count={}", 
                symbol, user.getId(), orders.size());
                
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