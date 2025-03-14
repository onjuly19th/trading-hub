package com.tradinghub.infrastructure.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.math.BigDecimal;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class ExternalCryptoMarketService extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExternalCryptoMarketService.class);
    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@ticker";
    private static final String BINANCE_REST_URL = "https://api.binance.com/api/v3/ticker/price?symbol=";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, BigDecimal> priceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Consumer<BigDecimal>> priceUpdateListeners = new ConcurrentHashMap<>();
    
    private WebSocketSession webSocketSession;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean isConnected = false;

    public ExternalCryptoMarketService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        connectWebSocket();
        // 연결 상태 모니터링 및 재연결 시도
        scheduler.scheduleAtFixedRate(this::checkAndReconnect, 5, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void cleanup() {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            try {
                webSocketSession.close();
            } catch (Exception e) {
                logger.error("Error closing WebSocket session", e);
            }
        }
        scheduler.shutdown();
    }

    private void connectWebSocket() {
        try {
            StandardWebSocketClient client = new StandardWebSocketClient();
            webSocketSession = client.execute(
                this,
                new WebSocketHttpHeaders(),
                URI.create(BINANCE_WS_URL)
            ).get();
            isConnected = true;
            logger.info("Successfully connected to Binance WebSocket");
        } catch (Exception e) {
            logger.error("Failed to connect to Binance WebSocket", e);
            isConnected = false;
        }
    }

    private void checkAndReconnect() {
        if (!isConnected || webSocketSession == null || !webSocketSession.isOpen()) {
            logger.info("Attempting to reconnect to Binance WebSocket");
            connectWebSocket();
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String symbol = root.get("s").asText();
            BigDecimal price = new BigDecimal(root.get("c").asText());
            
            updatePrice(symbol, price);
        } catch (Exception e) {
            logger.error("Error processing WebSocket message", e);
        }
    }

    private void updatePrice(String symbol, BigDecimal price) {
        priceCache.put(symbol, price);
        Consumer<BigDecimal> listener = priceUpdateListeners.get(symbol);
        if (listener != null) {
            listener.accept(price);
        }
    }

    public void addPriceUpdateListener(String symbol, Consumer<BigDecimal> listener) {
        priceUpdateListeners.put(symbol, listener);
    }

    public void removePriceUpdateListener(String symbol) {
        priceUpdateListeners.remove(symbol);
    }

    public BigDecimal getCurrentPrice(String symbol) {
        // BTC/USD -> BTCUSDT 형식으로 변환
        String formattedSymbol = formatSymbol(symbol);
        
        // 캐시에서 가격 조회
        BigDecimal cachedPrice = priceCache.get(formattedSymbol);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        // 캐시에 없는 경우 REST API로 조회
        try {
            String url = BINANCE_REST_URL + formattedSymbol;
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);
            BigDecimal price = new BigDecimal(root.get("price").asText());
            
            // 캐시 업데이트
            priceCache.put(formattedSymbol, price);
            return price;
        } catch (Exception e) {
            logger.error("Error fetching price for {} from REST API", symbol, e);
            throw new RuntimeException("Failed to fetch current price for " + symbol);
        }
    }

    private String formatSymbol(String symbol) {
        return symbol.replace("/", "") + "T";
    }

    public Map<String, Object> fetchMarketData() {
        String symbol = "BTCUSDT";
        BigDecimal price = getCurrentPrice("BTC/USD");
        
        Map<String, Object> marketData = new HashMap<>();
        marketData.put("symbol", symbol);
        marketData.put("price", price);
        marketData.put("timestamp", System.currentTimeMillis());
        
        return marketData;
    }
} 