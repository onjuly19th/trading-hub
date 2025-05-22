package com.tradinghub.application;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradinghub.application.service.order.OrderExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitOrderProcessor {
    private final OrderExecutionService orderExecutionService;

    public void processOrderEvent(String symbol, JsonNode orderData) {
        try {
            BigDecimal price = new BigDecimal(orderData.get("p").asText());
            
            // OrderExecutionService를 통해 체결 가능한 주문들을 처리
            orderExecutionService.checkAndExecuteOrders(symbol, price);
            
            log.debug("Processed trade event - symbol: {}, price: {}", symbol, price);
        } catch (Exception e) {
            log.error("Error processing trade event for symbol: {}", symbol, e);
        }
    }
}
