package com.tradinghub.application.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tradinghub.application.dto.ParsedBinanceMessage;

@Component
public class BinanceMessagePublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger log = LoggerFactory.getLogger(BinanceMessagePublisher.class);

    @Autowired
    public BinanceMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void handle(ParsedBinanceMessage message) {
        String destination = "/" + message.ticker() + "/" + message.streamType();
        log.debug("Publishing message to {}: {}", destination, message.data());
        messagingTemplate.convertAndSend(destination, message.data());
        log.debug("Successfully published message to {}", destination);
    }
}
