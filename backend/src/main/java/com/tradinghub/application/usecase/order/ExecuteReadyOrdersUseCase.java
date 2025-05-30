package com.tradinghub.application.usecase.order;

import com.fasterxml.jackson.databind.JsonNode;

public interface ExecuteReadyOrdersUseCase {
    void execute(String symbol, JsonNode orderData);
}
