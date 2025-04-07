package com.tradinghub.infrastructure.websocket;

import java.math.BigDecimal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.tradinghub.domain.trading.OrderExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 실시간 가격 업데이트를 처리하는 웹소켓 컨트롤러
 * 프론트엔드에서 바이낸스로부터 받은 가격 데이터를 수신하여 지정가 주문 체결에 사용
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PriceUpdateController {
    
    private final OrderExecutionService orderExecutionService;
    
    /**
     * 프론트엔드에서 전송한 가격 업데이트를 수신하여 처리
     * @param priceUpdate 가격 업데이트 데이터 (심볼, 가격)
     */
    @MessageMapping("/price-updates")
    public void processPriceUpdate(PriceUpdate priceUpdate) {
        if (priceUpdate == null || priceUpdate.getSymbol() == null || priceUpdate.getPrice() == null) {
            log.warn("Invalid price update received: {}", priceUpdate);
            return;
        }
        
        try {
            String symbol = priceUpdate.getSymbol();
            BigDecimal price = new BigDecimal(priceUpdate.getPrice());
            
            log.debug("Processing price update: {} = {}", symbol, price);
            
            // 지정가 주문 체결 처리
            orderExecutionService.checkAndExecuteTrades(symbol, price);
        } catch (Exception e) {
            log.error("Error processing price update: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 가격 업데이트 DTO
     */
    public static class PriceUpdate {
        private String symbol;
        private String price;
        
        public String getSymbol() {
            return symbol;
        }
        
        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }
        
        public String getPrice() {
            return price;
        }
        
        public void setPrice(String price) {
            this.price = price;
        }
        
        @Override
        public String toString() {
            return "PriceUpdate{symbol='" + symbol + "', price='" + price + "'}";
        }
    }
} 