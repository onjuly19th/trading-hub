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
        log.info("현재가 {}에서 체결 가능한 {}개의 주문 확인됨", currentPrice, executableOrders.size());
        
        // OrderService의 executeOrder 메소드를 활용하여 주문 체결
        executableOrders.forEach(orderService::executeOrder);
    }
} 