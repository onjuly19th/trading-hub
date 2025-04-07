package com.tradinghub.domain.trading;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.common.response.ApiResponse;
import com.tradinghub.domain.trading.dto.OrderRequest;
import com.tradinghub.domain.trading.dto.OrderResponse;
import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.security.CurrentUser;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    
    /**
     * 로그인한 사용자의 주문 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(@CurrentUser User user) {
        log.info("Fetching orders for user: {}", user.getUsername());
        
        try {
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            List<OrderResponse> orderResponses = orders.stream()
                .map(OrderResponse::new)
                .collect(Collectors.toList());
            
            log.info("Found {} orders for user: {}", orderResponses.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(orderResponses));
        } catch (Exception e) {
            log.error("Error fetching orders for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ORDER_FETCH_ERROR", "주문 목록을 불러오는데 실패했습니다."));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createOrder(@CurrentUser User user, @RequestBody OrderRequest request) {
        log.info("Creating {} order - User: {}, Symbol: {}", 
            request.getType(), user.getId(), request.getSymbol());
        
        validateOrderRequest(request);

        if (request.getType() == Order.OrderType.MARKET) {
            Order order = orderService.createMarketOrder(
                user.getId(),
                request.getSymbol(),
                request.getSide(),
                request.getPrice(),
                request.getAmount()
            );
            log.info("Market order executed - Order ID: {}", order.getId());
            return ResponseEntity.ok(ApiResponse.success(new OrderResponse(order)));
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
    
    /**
     * 주문 취소
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@CurrentUser User user, @PathVariable Long orderId) {
        log.info("Cancelling order - User: {}, Order ID: {}", user.getId(), orderId);
        
        try {
            orderService.cancelOrder(orderId, user.getId());
            log.info("Order cancelled successfully - Order ID: {}", orderId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("Error cancelling order: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("ORDER_CANCEL_ERROR", e.getMessage()));
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

    /**
     * 로그인한 사용자의 거래 내역 조회 (완료된 주문)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrderHistory(@CurrentUser User user) {
        log.info("Fetching order history for user: {}", user.getUsername());
        
        try {
            List<Order> completedOrders = orderRepository.findByUserIdAndStatusInOrderByCreatedAtDesc(
                user.getId(), 
                List.of(Order.OrderStatus.FILLED, Order.OrderStatus.CANCELLED)
            );
            
            List<OrderResponse> orderResponses = completedOrders.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
            
            log.info("Found {} completed orders for user: {}", orderResponses.size(), user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(orderResponses));
        } catch (Exception e) {
            log.error("Error fetching order history for user: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ORDER_HISTORY_FETCH_ERROR", "거래 내역을 불러오는데 실패했습니다."));
        }
    }
    
    /**
     * 특정 심볼의 거래 내역 조회
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersBySymbol(
            @CurrentUser User user, 
            @PathVariable String symbol) {
        log.info("Fetching orders for user: {} and symbol: {}", user.getUsername(), symbol);
        
        try {
            List<Order> orders = orderRepository.findByUserIdAndSymbolOrderByCreatedAtDesc(user.getId(), symbol);
            List<OrderResponse> orderResponses = orders.stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
            
            log.info("Found {} orders for user: {} and symbol: {}", 
                    orderResponses.size(), user.getUsername(), symbol);
            return ResponseEntity.ok(ApiResponse.success(orderResponses));
        } catch (Exception e) {
            log.error("Error fetching orders for user: {} and symbol: {}", 
                    user.getUsername(), symbol, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("ORDER_FETCH_ERROR", "주문 내역을 불러오는데 실패했습니다."));
        }
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        
        if (request.getPrice() != null && request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        
        if (request.getType() == Order.OrderType.LIMIT && request.getPrice() == null) {
            throw new IllegalArgumentException("Price is required for limit orders");
        }
    }
} 