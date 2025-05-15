package com.tradinghub.infrastructure.external;

import org.springframework.stereotype.Component;

import com.tradinghub.application.dto.ParsedBinanceMessage;
import com.tradinghub.application.handler.BinanceMessagePublisher;
import com.tradinghub.application.parser.BinanceMessageParser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceWebSocketHandler {

    private final BinanceMessageParser parser;
    private final BinanceMessagePublisher publisher;

    public void handleMessage(String payload) {
        try {
            ParsedBinanceMessage message = parser.parse(payload);
            publisher.handle(message);
        } catch (Exception e) {
            log.error("Error parsing Binance message", e);
        }
    }
}