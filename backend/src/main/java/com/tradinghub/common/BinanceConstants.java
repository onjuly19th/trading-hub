package com.tradinghub.common;

import java.util.List;
import java.util.stream.Collectors;

public class BinanceConstants {
    
    private static final List<String> SYMBOLS = List.of(
    "btcusdt", "ethusdt", "xrpusdt", "bnbusdt", "solusdt",
    "trxusdt", "dogeusdt", "adausdt", "xlmusdt", "linkusdt"
    );

    private static final List<String> STREAM_TYPES = List.of(
    "trade", "ticker", "depth20"
    );

    private static String buildBinanceStreamUrl(List<String> symbols, List<String> streamTypes) {
        String streams = symbols.stream()
            .flatMap(symbol -> streamTypes.stream().map(type -> symbol + "@" + type))
            .collect(Collectors.joining("/"));
        return "wss://stream.binance.com:9443/stream?streams=" + streams;
    }

    private static final String BINANCE_STREAM_URL = buildBinanceStreamUrl(SYMBOLS, STREAM_TYPES);

    public static String getBinanceStreamUrl() {
        return BINANCE_STREAM_URL;
    }
}
