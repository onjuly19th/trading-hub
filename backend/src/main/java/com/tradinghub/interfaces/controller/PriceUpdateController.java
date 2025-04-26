package com.tradinghub.interfaces.controller;

import java.math.BigDecimal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import com.tradinghub.application.service.order.OrderExecutionService;
import com.tradinghub.infrastructure.exception.InvalidPriceFormatException;
import com.tradinghub.infrastructure.exception.PriceUpdateProcessingException;
import com.tradinghub.interfaces.dto.order.PriceUpdate;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 실시간 가격 업데이트를 처리하는 웹소켓 컨트롤러
 * 프론트엔드에서 바이낸스로부터 받은 가격 데이터를 수신하여 지정가 주문 체결에 사용
 */
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
        String symbol = priceUpdate.getSymbol();
        BigDecimal price;
        
        try {
            price = new BigDecimal(priceUpdate.getPrice());
        } catch (NumberFormatException e) {
            throw new InvalidPriceFormatException("Invalid price format: " + priceUpdate.getPrice(), e);
        }
        
        try {
            orderExecutionService.checkAndExecuteOrders(symbol, price);
        } catch (Exception e) {
            throw new PriceUpdateProcessingException("Failed to process price update for symbol: " + symbol, e);
        }
    }
} 