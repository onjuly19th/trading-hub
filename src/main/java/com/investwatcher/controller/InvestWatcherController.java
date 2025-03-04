package com.investwatcher.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.investwatcher.service.CryptoService;
import com.investwatcher.service.StockService;

@RestController
public class InvestWatcherController {
    private final CryptoService cryptoService;
    private final StockService stockService;

    public InvestWatcherController(CryptoService cryptoService, StockService stockService) {
        this.cryptoService = cryptoService;
        this.stockService = stockService;
    }

    @GetMapping("/crypto")
    public String getCryptoPrice(@RequestParam String symbol) {
        return cryptoService.getCryptoPrice(symbol);
    }

    @GetMapping("/stock")
    public String getStockPrice(@RequestParam String symbol) {
        return stockService.getStockPrice(symbol);
    }    
}
