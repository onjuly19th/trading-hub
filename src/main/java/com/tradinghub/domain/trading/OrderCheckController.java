package com.tradinghub.domain.trading;

import java.math.BigDecimal;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.tradinghub.domain.trading.dto.OrderCheckRequest;
import com.tradinghub.domain.trading.dto.OrderCheckResponse;

@RestController
@RequestMapping("/api/trading/check")
@RequiredArgsConstructor
@Slf4j
public class OrderCheckController {
    private final TradeExecutionService tradeExecutionService;
    private final OrderRepository orderRepository;
    
    // 마지막 가격 출력 시간 기록용 변수
    private long lastPriceLogTime = 0L;
    private static final long PRICE_LOG_INTERVAL = 10000; // 10초 간격으로만 로깅
    
    /**
     * 통신 테스트용 간단한 엔드포인트 
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "통신 테스트 성공");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(response);
    }
    
    /**
     * 프론트엔드에서 받은 실시간 가격으로 지정가 주문 체결 가능 여부를 확인하고 처리
     * 
     * @param request 가격 정보 요청 객체
     * @return 처리 결과
     */
    @PostMapping(value = "/price", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderCheckResponse> checkOrdersWithCurrentPrice(@RequestBody OrderCheckRequest request) {
        try {
            // 요청 파라미터 검증
            if (request.getSymbol() == null || request.getSymbol().isEmpty()) {
                log.error("심볼이 비어있습니다");
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(OrderCheckResponse.error("심볼이 비어있습니다"));
            }
            
            if (request.getPrice() == null || request.getPrice().isEmpty()) {
                log.error("가격이 비어있습니다");
                return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(OrderCheckResponse.error("가격이 비어있습니다"));
            }
            
            // 가격 로깅은 일정 간격으로만 수행 (너무 많은 로그 방지)
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastPriceLogTime > PRICE_LOG_INTERVAL) {
                log.info("실시간 가격: {} - {}", request.getSymbol(), request.getPrice());
                lastPriceLogTime = currentTime;
            }
            
            // 문자열을 BigDecimal로 변환
            BigDecimal currentPrice = new BigDecimal(request.getPrice());
            
            // 체결 가능한 주문 찾기
            List<Order> executableOrders = orderRepository.findExecutableOrders(request.getSymbol(), currentPrice);
            
            // 체결 가능한 주문이 있을 때만 로깅
            if (!executableOrders.isEmpty()) {
                log.info("체결 가능한 주문 발견: {} 개, 심볼: {}, 현재가: {}", 
                        executableOrders.size(), request.getSymbol(), currentPrice);
                
                for (Order order : executableOrders) {
                    log.info("체결 예정 주문: ID={}, 타입={}, 가격={}", 
                            order.getId(), order.getType(), order.getPrice());
                }
            }
            
            // 현재 가격에 맞는 주문 체결 처리
            tradeExecutionService.checkAndExecuteTrades(request.getSymbol(), currentPrice);
            
            // 응답 생성
            OrderCheckResponse response;
            if (executableOrders.isEmpty()) {
                response = OrderCheckResponse.success("주문 체결 확인 완료", 0);
            } else {
                response = OrderCheckResponse.success(
                    executableOrders.size() + "개의 주문 체결 처리 중", 
                    executableOrders.size()
                );
            }
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
            
        } catch (NumberFormatException e) {
            log.error("가격 형식 오류: {}", request.getPrice(), e);
            return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(OrderCheckResponse.error("가격 형식이 올바르지 않습니다"));
                
        } catch (Exception e) {
            log.error("주문 체결 확인 중 오류 발생", e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(OrderCheckResponse.error("주문 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
} 