package com.tradinghub.domain.trading.controller;

import java.math.BigDecimal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.tradinghub.domain.trading.OrderExecutionService;
import com.tradinghub.domain.trading.dto.PriceUpdate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실시간 가격 업데이트를 처리하는 웹소켓 컨트롤러
 * 프론트엔드에서 바이낸스로부터 받은 가격 데이터를 수신하여 지정가 주문 체결에 사용
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@Validated
public class PriceUpdateController {
    
    private final OrderExecutionService orderExecutionService;
    
    /**
     * 프론트엔드에서 전송한 가격 업데이트를 수신하여 처리
     * @param priceUpdate 가격 업데이트 데이터 (심볼, 가격)
     */
    @MessageMapping("/price-updates")
    public void processPriceUpdate(@Valid PriceUpdate priceUpdate) {
        try {
            String symbol = priceUpdate.getSymbol();
            BigDecimal price = new BigDecimal(priceUpdate.getPrice());
            
            log.debug("Processing price update: symbol={}, price={}", symbol, price);
            
            // 지정가 주문 체결 처리
            orderExecutionService.checkAndExecuteOrders(symbol, price);
            
            // 로그를 DEBUG 레벨로 변경하여 로그 출력 최소화
            log.debug("Price update processed: symbol={}, price={}", symbol, price);
        } catch (Exception e) {
            log.error("Error processing price update: error={}", e.getMessage(), e);
        }
    }
} 