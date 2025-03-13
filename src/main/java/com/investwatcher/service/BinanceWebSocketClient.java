package com.investwatcher.service;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class BinanceWebSocketClient extends WebSocketClient {

    private final SimpMessagingTemplate messagingTemplate;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> reconnectTask;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public BinanceWebSocketClient(SimpMessagingTemplate messagingTemplate) throws URISyntaxException {
        super(new URI("wss://stream.binance.com:9443/ws/btcusdt@ticker"));
        this.messagingTemplate = messagingTemplate;
        initScheduler();
    }

    private void initScheduler() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = new ScheduledThreadPoolExecutor(1);
        }
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.info("Binance WebSocket 연결됨");
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    @Override
    public void onMessage(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/price", message);
            log.debug("가격 업데이트: {}", message);
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: ", e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("Binance WebSocket 연결 종료: {}. 재연결 시도...", reason);
        if (!isShuttingDown.get()) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        log.error("Binance WebSocket 오류 발생: ", ex);
        if (!isShuttingDown.get()) {
            scheduleReconnect();
        }
    }

    private synchronized void scheduleReconnect() {
        try {
            initScheduler();
            if (reconnectTask == null || reconnectTask.isDone()) {
                reconnectTask = scheduler.scheduleWithFixedDelay(() -> {
                    try {
                        if (!isOpen() && !isShuttingDown.get()) {
                            log.info("재연결 시도 중...");
                            reconnect();
                        }
                    } catch (Exception e) {
                        log.error("재연결 실패: ", e);
                    }
                }, 0, 5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("재연결 스케줄링 실패: ", e);
        }
    }

    public void cleanup() {
        isShuttingDown.set(true);
        if (reconnectTask != null) {
            reconnectTask.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        try {
            closeBlocking();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("WebSocket 종료 중 인터럽트 발생", e);
        }
    }
} 