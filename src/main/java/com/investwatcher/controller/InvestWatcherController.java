package com.investwatcher.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.investwatcher.service.CryptoService;
import com.investwatcher.service.StockService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class InvestWatcherController {
    private final CryptoService cryptoService;
    private final StockService stockService;

    public InvestWatcherController(CryptoService cryptoService, StockService stockService) {
        this.cryptoService = cryptoService;
        this.stockService = stockService;
    }

    @GetMapping("/crypto")
    public Map<String, Object> getCryptoPrice(@RequestParam String symbol) {
        String priceInfo = cryptoService.getCryptoPrice(symbol);
        return parsePriceInfo(priceInfo);        
    }

    @GetMapping("/stock")
    public Map<String, Object> getStockPrice(@RequestParam String symbol) {
        String priceInfo = stockService.getStockPrice(symbol);
        return parsePriceInfo(priceInfo);
    }
    
    private Map<String, Object> parsePriceInfo(String priceInfo) {
        String[] parts = priceInfo.split(", ");
        Map<String, Object> response = new HashMap<>();
        response.put("USD", Double.parseDouble(parts[0].split(": ")[1]));
        response.put("KRW", Double.parseDouble(parts[1].split(": ")[1]));
        response.put("image", parts[2].split(": ")[1]);
        return response;
    }
}