package com.tradinghub.interfaces.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tradinghub.application.usecase.order.GetClosedOrdersUseCase;
import com.tradinghub.application.usecase.order.GetOpenOrdersUseCase;
import com.tradinghub.domain.model.order.Order;
import com.tradinghub.domain.model.user.User;
import com.tradinghub.interfaces.dto.order.OrderResponse;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderQueryController {
    private final GetOpenOrdersUseCase getOpenOrdersUseCase;
    private final GetClosedOrdersUseCase getClosedOrdersUseCase;
    
    /**
     * 로그인한 사용자의 열린 주문 (미체결) 목록을 조회합니다.
     * 
     * @param authentication 인증 정보
     * @return 미체결 주문 목록
     * @response 200 성공
     * @response 401 인증되지 않은 사용자
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOpenOrders(@AuthenticationPrincipal User user) {
        List<Order> orders = getOpenOrdersUseCase.execute(user.getId());
        List<OrderResponse> orderResponses = OrderResponse.fromList(orders);
        
        return ResponseEntity.ok(orderResponses);
    }
    
    /**
     * 로그인한 사용자의 닫힌 주문(체결 또는 취소) 목록을 조회합니다.
     * 
     * @param authentication 인증 정보
     * @return 완료된 주문 목록
     * @response 200 조회 성공
     * @response 401 인증되지 않은 사용자
     */
    @GetMapping("/history")
    public ResponseEntity<List<OrderResponse>> getClosedOrders(@AuthenticationPrincipal User user) {
        List<Order> completedOrders = getClosedOrdersUseCase.execute(user.getId());
        List<OrderResponse> orderResponses = OrderResponse.fromList(completedOrders);
        
        return ResponseEntity.ok(orderResponses);
    }
}
