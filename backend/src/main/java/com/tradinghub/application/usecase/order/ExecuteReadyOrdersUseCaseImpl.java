package com.tradinghub.application.usecase.order;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.tradinghub.domain.model.order.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteReadyOrdersUseCaseImpl implements ExecuteReadyOrdersUseCase {
    private final OrderRepository orderRepository;
    private final ExecuteOrderUseCase executeOrderUseCase;

    @Override
    @Transactional
    public void execute(String symbol, JsonNode orderData) {
        try {
            BigDecimal price = new BigDecimal(orderData.get("p").asText());
            
            var executableOrders = orderRepository.findExecutableOrders(symbol, price);
        
            if (!executableOrders.isEmpty()) {
                executableOrders.forEach(order -> {
                    executeOrderUseCase.execute(order);
                });
            }
        } catch (Exception e) {
            log.error("Error processing trade event for symbol: {}", symbol, e);
        }
    }

}
