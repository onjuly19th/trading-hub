package com.tradinghub.infrastructure.websocket;

import com.tradinghub.domain.trading.TradeExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoMarketService {
    private final CryptoMarketWebSocketHandler webSocketHandler;
    private final TradeExecutionService tradeExecutionService;
    private final Map<String, ConcurrentHashMap<String, Consumer<BigDecimal>>> priceListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BigDecimal> priceCache = new ConcurrentHashMap<>();

    /**
     * 현재 가격을 조회합니다.
     * WebSocket으로 실시간 업데이트된 가격을 반환하며,
     * 캐시에 없는 경우 0을 반환합니다.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return priceCache.getOrDefault(symbol, BigDecimal.ZERO);
    }

    /**
     * 가격 업데이트 리스너를 등록합니다.
     * 등록된 리스너는 해당 심볼의 가격이 변경될 때마다 호출됩니다.
     */
    public void addPriceUpdateListener(String symbol, String listenerId, Consumer<BigDecimal> listener) {
        priceListeners.computeIfAbsent(symbol, k -> new ConcurrentHashMap<>())
                     .put(listenerId, listener);
    }

    /**
     * 가격 업데이트 리스너를 제거합니다.
     */
    public void removePriceUpdateListener(String symbol, String listenerId) {
        if (priceListeners.containsKey(symbol)) {
            priceListeners.get(symbol).remove(listenerId);
        }
    }

    /**
     * 가격 업데이트를 처리합니다.
     * 1. 캐시 업데이트
     * 2. 웹소켓 클라이언트들에게 브로드캐스트
     * 3. 등록된 리스너들에게 알림
     * 4. 지정가 주문 체결 확인
     */
    public void handlePriceUpdate(String symbol, BigDecimal price) {
        // 캐시 업데이트
        priceCache.put(symbol, price);
        
        // 웹소켓 클라이언트들에게 가격 정보 브로드캐스트
        broadcastPrice(symbol, price);
        
        // 등록된 리스너들에게 알림
        notifyPriceListeners(symbol, price);
        
        // 지정가 주문 체결 확인
        tradeExecutionService.checkAndExecuteTrades(symbol, price);
        
        log.debug("Price updated for {}: {}", symbol, price);
    }

    private void broadcastPrice(String symbol, BigDecimal price) {
        webSocketHandler.broadcastMarketData(symbol, price);
    }

    private void notifyPriceListeners(String symbol, BigDecimal price) {
        if (priceListeners.containsKey(symbol)) {
            priceListeners.get(symbol).values().forEach(listener -> {
                try {
                    listener.accept(price);
                } catch (Exception e) {
                    log.error("Error notifying price listener for {}: {}", symbol, e.getMessage());
                }
            });
        }
    }
} 