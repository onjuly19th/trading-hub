package com.tradinghub.domain.trading;

import com.tradinghub.domain.user.User;
import com.tradinghub.infrastructure.security.CurrentUser;
import com.tradinghub.domain.trading.dto.OrderRequest;
import com.tradinghub.domain.trading.dto.OrderResponse;
import com.tradinghub.domain.trading.dto.TradeResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@CurrentUser User user, @RequestBody OrderRequest request) {
        log.info("Order request received - User: {}, Symbol: {}, Type: {}, Side: {}, Amount: {}, Price: {}",
            user.getId(), request.getSymbol(), request.getType(), request.getSide(), 
            request.getAmount(), request.getPrice());
        
        validateOrderRequest(request);

        if (request.getType() == Order.OrderType.MARKET) {
            log.info("Processing market order for user: {}", user.getId());
            Trade trade = orderService.createMarketOrder(
                user.getId(),
                request.getSymbol(),
                request.getSide(),
                request.getPrice(),
                request.getAmount()
            );
            log.info("Market order processed successfully - Trade ID: {}", trade.getId());
            return ResponseEntity.ok(TradeResponse.from(trade));
        } else {
            log.info("Processing limit order for user: {}", user.getId());
            Order order = orderService.createLimitOrder(
                user.getId(),
                request.getSymbol(),
                request.getSide(),
                request.getPrice(),
                request.getAmount()
            );
            log.info("Limit order processed successfully - Order ID: {}", order.getId());
            return ResponseEntity.ok(new OrderResponse(order));
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders(@CurrentUser User user) {
        List<Order> orders = orderService.getUserOrders(user.getId());
        return ResponseEntity.ok(orders.stream()
            .map(OrderResponse::new)
            .toList());
    }

    @GetMapping("/book/{symbol}")
    public ResponseEntity<List<OrderResponse>> getOrderBook(@PathVariable String symbol) {
        List<Order> orders = orderService.getOrderBook(symbol);
        return ResponseEntity.ok(orders.stream()
            .map(OrderResponse::new)
            .toList());
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(@CurrentUser User user, @PathVariable Long orderId) {
        orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.ok().build();
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