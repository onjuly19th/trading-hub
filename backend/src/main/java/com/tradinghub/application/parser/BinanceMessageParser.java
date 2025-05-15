package com.tradinghub.application.parser;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradinghub.application.dto.ParsedBinanceMessage;

@Component
public class BinanceMessageParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParsedBinanceMessage parse(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        String stream = root.get("stream").asText();
        JsonNode data = root.get("data");
        return new ParsedBinanceMessage(stream, data);
    }
}
