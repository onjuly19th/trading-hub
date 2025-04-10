package com.tradinghub.domain.trading;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 체결 서비스
 * 지정가 주문의 체결을 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExecutionService {
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    /**
     * 현재가에 따라 체결 가능한 지정가 주문들을 확인하고 거래 체결
     * 매수 주문: 현재가가 지정가 이하일 때 체결
     * 매도 주문: 현재가가 지정가 이상일 때 체결
     *
     * @param symbol 거래 심볼 (예: BTC/USDT)
     * @param currentPrice 현재 시장 가격
     */
    @Transactional
    public void checkAndExecuteOrders(String symbol, BigDecimal currentPrice) {
        var executableOrders = orderRepository.findExecutableOrders(symbol, currentPrice);
        
        // 실행 가능한 주문이 있을 때만 INFO 로그 출력
        if (!executableOrders.isEmpty()) {
            log.info("Found {} executable orders at current price {} for {}", 
                    executableOrders.size(), currentPrice, symbol);
            
            // OrderService의 executeOrder 메소드를 활용하여 주문 체결
            executableOrders.forEach(orderService::executeOrder);
        } else {
            // 주문이 없는 경우 DEBUG 레벨로 로깅
            log.debug("No executable orders at current price {} for {}", currentPrice, symbol);
        }
    }
} 