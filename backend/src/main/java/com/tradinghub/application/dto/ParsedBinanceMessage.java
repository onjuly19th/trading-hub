package com.tradinghub.application.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ParsedBinanceMessage(String stream, JsonNode data) {

    public String symbol() {
        int atIndex = stream.indexOf("@");
        return atIndex != -1 ? stream.substring(0, atIndex) : stream;
    }

    public String ticker() {
        String symbol = symbol();
        if (symbol.toLowerCase().endsWith("usdt")) {
            return symbol.substring(0, symbol.length() - 4).toLowerCase();
        }
        return symbol.toLowerCase();
    }

    public String streamType() {
        int atIndex = stream.indexOf("@");
        return atIndex != -1 ? stream.substring(atIndex + 1) : "";
    }
}
