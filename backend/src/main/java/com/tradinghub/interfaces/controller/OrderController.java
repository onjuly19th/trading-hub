package com.tradinghub.interfaces.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tradinghub.application.exception.order.InvalidOrderException;
import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.application.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.order.OrderApplicationService;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.domain.repository.UserRepository;
import com.tradinghub.interfaces.dto.order.OrderRequest;
import com.tradinghub.interfaces.dto.order.OrderResponse;
import com.tradinghub.interfaces.exception.auth.UnauthorizedOperationException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 주문 관련 REST API 엔드포인트를 제공하는 컨트롤러
 * 
 * 제공하는 기능:
 * 1. 주문 생성 (시장가/지정가)
 * 2. 주문 조회 (전체/심볼별/히스토리)
 * 3. 주문 취소
 * 4. 주문 실행 (관리자용)
 * 
 * 모든 엔드포인트는 인증된 사용자만 접근 가능합니다.
 * 
 * @see OrderApplicationService 주문 처리 서비스
 * @see OrderRequest 주문 생성 요청 DTO
 * @see OrderResponse 주문 응답 DTO
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Order Management", description = "주문 생성, 조회, 취소 관련 API")
public class OrderController {
    private final OrderApplicationService orderService;
    private final UserRepository userRepository;
    
    // -----------------------------
    // Create Operations
    // -----------------------------
    
    /**
     * 새로운 주문을 생성합니다.
     * 시장가 주문은 즉시 체결되며, 지정가 주문은 가격 조건이 맞을 때 체결됩니다.
     * 
     * @param request 주문 생성 요청 정보
     * @param authentication 인증 정보
     * @return 생성된 주문 정보
     * @throws InvalidOrderException 주문 정보가 유효하지 않은 경우
     * @throws InsufficientBalanceException 잔고가 부족한 경우
     * @response 200 주문 생성 성공
     * @response 400 잘못된 요청 (유효하지 않은 주문 정보)
     * @response 401 인증되지 않은 사용자
     * @response 403 권한 없음
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        
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
        
        return ResponseEntity.ok(OrderResponse.from(order));
    }

    /**
     * 특정 가격에서 체결 가능한 모든 지정가 주문을 실행합니다.
     * 관리자 전용 API입니다.
     * 
     * @param symbol 거래 심볼
     * @param price 현재 시장 가격
     * @return 체결된 주문 수 정보
     * @response 200 실행 성공
     * @response 401 인증되지 않은 사용자
     * @response 403 권한 없음
     */
    @PostMapping("/execute")
    public ResponseEntity<OrderResponse> executeOrders(
            @RequestParam @NotBlank(message = "Symbol is required") String symbol,
            @RequestParam @DecimalMin(value = "0.00000001", 
                                    message = "Price must be greater than 0") BigDecimal price) {
        int executedCount = orderService.executeOrdersAtPrice(symbol, price);
        String message = String.format("Executed %d orders", executedCount);
        
        return ResponseEntity.ok(OrderResponse.withMessage(message, executedCount));
    }

    // -----------------------------
    // Read Operations
    // -----------------------------
    
    /**
     * 로그인한 사용자의 미체결 주문 목록을 조회합니다.
     * 대기 중이거나 일부 체결된 주문만 포함됩니다.
     * 
     * @param authentication 인증 정보
     * @return 미체결 주문 목록
     * @response 200 성공
     * @response 401 인증되지 않은 사용자
     */
    @GetMapping
    @Operation(summary = "사용자 미체결 주문 조회", description = "로그인한 사용자의 PENDING 또는 PARTIALLY_FILLED 상태인 주문 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    public ResponseEntity<List<OrderResponse>> getUserOrders(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<Order> orders = orderService.getPendingOrdersByUserId(user.getId());
        List<OrderResponse> orderResponses = OrderResponse.fromList(orders);
        
        return ResponseEntity.ok(orderResponses);
    }
    
    /**
     * 로그인한 사용자의 거래 내역(완료된 주문)을 조회합니다.
     * 체결되거나 취소된 주문만 포함됩니다.
     * 
     * @param authentication 인증 정보
     * @return 완료된 주문 목록
     * @response 200 조회 성공
     * @response 401 인증되지 않은 사용자
     */
    @GetMapping("/history")
    @Operation(summary = "사용자 거래 내역 조회", description = "로그인한 사용자의 완료(FILLED, CANCELLED)된 주문 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    public ResponseEntity<List<OrderResponse>> getOrderHistory(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<Order> completedOrders = orderService.getCompletedOrdersByUserId(user.getId());
        
        return ResponseEntity.ok(OrderResponse.fromList(completedOrders));
    }
    
    /**
     * 특정 심볼에 대한 사용자의 주문 내역을 조회합니다.
     * 
     * @param symbol 조회할 심볼
     * @param authentication 인증 정보
     * @return 해당 심볼의 주문 목록
     * @response 200 조회 성공
     * @response 401 인증되지 않은 사용자
     */
    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<OrderResponse>> getOrdersBySymbol(
            @PathVariable String symbol, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        List<Order> orders = orderService.getOrdersByUserIdAndSymbol(user.getId(), symbol);
        
        return ResponseEntity.ok(OrderResponse.fromList(orders));
    }

    // -----------------------------
    // Delete Operations
    // -----------------------------
    
    /**
     * 대기 중인 주문을 취소합니다.
     * 이미 체결된 주문은 취소할 수 없습니다.
     * 
     * @param orderId 취소할 주문 ID
     * @param authentication 인증 정보
     * @return void
     * @throws OrderNotFoundException 주문을 찾을 수 없는 경우
     * @throws UnauthorizedOperationException 권한이 없는 경우
     * @throws InvalidOrderException 이미 체결/취소된 주문인 경우
     * @response 200 취소 성공
     * @response 401 인증되지 않은 사용자
     * @response 403 권한 없음
     * @response 404 주문 없음
     */
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        orderService.cancelOrder(orderId, user.getId());
        
        return ResponseEntity.ok().build();
    }

    // -----------------------------
    // Private Helper Methods
    // -----------------------------
    
    /**
     * 주문 요청의 유효성을 검증합니다.
     * Bean Validation으로 처리할 수 없는 비즈니스 규칙을 검증합니다.
     * 
     * @param request 검증할 주문 요청
     * @throws InvalidOrderException 지정가 주문에 가격이 지정되지 않은 경우
     */
    private void validateOrderRequest(OrderRequest request) {
        // Bean Validation은 이미 amount > 0인지 확인하므로 제거
        
        // Bean Validation은 price > 0인지 확인하므로 제거
        
        // LIMIT 주문에 price가 필요한지 확인 (Bean Validation으로는 확인할 수 없음)
        if (request.getType() == Order.OrderType.LIMIT && request.getPrice() == null) {
            throw new InvalidOrderException("Limit order requires a price");
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
             throw new UnauthorizedOperationException("User is not authenticated.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        } else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> new UnauthorizedOperationException("User not found with username: " + username));
        } else if (principal instanceof String && principal.equals("anonymousUser")) {
             throw new UnauthorizedOperationException("Anonymous access is not allowed.");
        }

        // 예상치 못한 Principal 타입 처리
        throw new UnauthorizedOperationException("Cannot determine user from principal: " + principal.getClass().getName());
    }
} 