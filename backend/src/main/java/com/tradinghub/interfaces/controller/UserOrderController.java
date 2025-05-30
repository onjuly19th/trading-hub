package com.tradinghub.interfaces.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tradinghub.application.exception.order.InvalidOrderException;
import com.tradinghub.application.exception.order.OrderNotFoundException;
import com.tradinghub.application.exception.portfolio.InsufficientBalanceException;
import com.tradinghub.application.usecase.order.CancelOrderUseCase;
import com.tradinghub.application.usecase.order.PlaceOrderUseCase;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.dto.order.OrderRequest;
import com.tradinghub.interfaces.dto.order.OrderResponse;
import com.tradinghub.interfaces.exception.auth.UnauthorizedOperationException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class UserOrderController {
    private final PlaceOrderUseCase placeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

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
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal User user) {
        
        Order order = placeOrderUseCase.execute(request.toCommand(user));        
        return ResponseEntity.ok(OrderResponse.from(order));
    }
        
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
            @PathVariable Long orderId,
            @AuthenticationPrincipal User user) {
        
        cancelOrderUseCase.execute(orderId, user.getId());
        return ResponseEntity.ok().build();
    }
}
