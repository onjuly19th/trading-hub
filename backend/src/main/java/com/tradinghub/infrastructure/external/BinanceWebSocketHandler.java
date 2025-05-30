package com.tradinghub.infrastructure.external;

import org.springframework.stereotype.Component;

import com.tradinghub.application.dto.ParsedBinanceMessage;
import com.tradinghub.application.handler.BinanceMessagePublisher;
import com.tradinghub.application.parser.BinanceMessageParser;
import com.tradinghub.application.usecase.order.ExecuteReadyOrdersUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceWebSocketHandler {

    private final BinanceMessageParser parser;
    private final BinanceMessagePublisher publisher;
    private final ExecuteReadyOrdersUseCase executeReadyOrdersUseCase;

    public void handleMessage(String payload) {
        try {
            ParsedBinanceMessage message = parser.parse(payload);
            
            // trade 스트림인 경우 지정가 주문 처리
            if ("trade".equals(message.streamType())) {
                executeReadyOrdersUseCase.execute(message.symbol(), message.data());
            }
            
            publisher.handle(message);
        } catch (Exception e) {
            log.error("Error parsing Binance message", e);
        }
    }
}