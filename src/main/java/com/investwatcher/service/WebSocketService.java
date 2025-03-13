package com.investwatcher.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final BinanceWebSocketClient webSocketClient;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 5000;

    @PostConstruct
    public void connect() {
        int retryCount = 0;
        boolean connected = false;

        while (!connected && retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                webSocketClient.connectBlocking();
                connected = true;
                log.info("WebSocket 서비스 시작됨");
            } catch (InterruptedException e) {
                log.error("WebSocket 연결 중 오류 발생: ", e);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                retryCount++;
                log.error("WebSocket 연결 실패 (시도 {}/{}): {}", retryCount, MAX_RETRY_ATTEMPTS, e.getMessage());
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        if (!connected) {
            log.error("최대 재시도 횟수 초과. WebSocket 연결 실패");
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            webSocketClient.cleanup();
            log.info("WebSocket 서비스 종료됨");
        } catch (Exception e) {
            log.error("WebSocket 종료 중 오류 발생: ", e);
        }
    }
} 