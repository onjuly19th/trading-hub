package com.investwatcher.controller;

import com.investwatcher.model.CryptoCurrency;
import com.investwatcher.service.CryptoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/crypto")
public class CryptoController {

    private final CryptoService cryptoService;

    public CryptoController(CryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @GetMapping("/{symbol}")
    public Mono<CryptoCurrency> getCryptoPrice(@PathVariable String symbol) {
        return cryptoService.fetchAndSavePrice(symbol.toUpperCase());
    }
} 