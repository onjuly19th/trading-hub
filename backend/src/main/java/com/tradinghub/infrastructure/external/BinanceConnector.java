package com.tradinghub.infrastructure.external;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceConnector {

    private static final String BINANCE_STREAM_URL = "wss://stream.binance.com:9443/stream?streams=" +
    "btcusdt@trade/btcusdt@ticker/btcusdt@depth20" +
    "/ethusdt@trade/ethusdt@ticker/ethusdt@depth20" +
    "/xrpusdt@trade/xrpusdt@ticker/xrpusdt@depth20" +
    "/bnbusdt@trade/bnbusdt@ticker/bnbusdt@depth20" +
    "/solusdt@trade/solusdt@ticker/solusdt@depth20" +
    "/trxusdt@trade/trxusdt@ticker/trxusdt@depth20" +
    "/dogeusdt@trade/dogeusdt@ticker/dogeusdt@depth20" +
    "/adausdt@trade/adausdt@ticker/adausdt@depth20" +
    "/xlmusdt@trade/xlmusdt@ticker/xlmusdt@depth20" +
    "/linkusdt@trade/linkusdt@ticker/linkusdt@depth20";
    private static final long RECONNECT_DELAY_SEC = 5;
    private static final long PING_INTERVAL_SEC = 30;

    private final BinanceWebSocketHandler handler;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private WebSocketSession session;

    /**
     * 현재 웹소켓 연결 상태를 확인합니다.
     * @return 연결이 활성화되어 있으면 true, 그렇지 않으면 false
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    @PostConstruct
    public void init() {
        connectWithRetry();
        startPingTask();
    }

    private void connectWithRetry() {
        try {
            WebSocketClient client = new StandardWebSocketClient();
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    BinanceConnector.this.session = session;
                    log.info("Connected to Binance WebSocket.");
                }

                @Override
                public void handleTextMessage(WebSocketSession session, TextMessage message) {
                    handler.handleMessage(message.getPayload());
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    log.error("WebSocket transport error", exception);
                    reconnectWithDelay();
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    log.warn("WebSocket connection closed: {}", status);
                    reconnectWithDelay();
                }
            }, new WebSocketHttpHeaders(), URI.create(BINANCE_STREAM_URL));

        } catch (Exception e) {
            log.error("WebSocket connection failed, retrying...", e);
            reconnectWithDelay();
        }
    }

    private void reconnectWithDelay() {
        scheduler.schedule(this::connectWithRetry, RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
    }

    private void startPingTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (session != null && session.isOpen()) {
                    session.sendMessage(new PingMessage());
                    log.debug("Ping message sent.");
                }
            } catch (Exception e) {
                log.error("Failed to send ping", e);
            }
        }, PING_INTERVAL_SEC, PING_INTERVAL_SEC, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            log.error("Error closing WebSocket session", e);
        }
        scheduler.shutdown();
    }
}
