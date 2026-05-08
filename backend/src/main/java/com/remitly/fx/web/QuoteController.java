package com.remitly.fx.web;

import com.remitly.fx.model.QuoteResponse;
import com.remitly.fx.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final ExchangeRateService service;

    public QuoteController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping
    public QuoteResponse quote(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        return service.quote(from, to, amount);
    }

    @PostMapping("/refresh")
    public ResponseEntity<QuoteResponse> refresh(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {
        service.refresh(from, to);
        return ResponseEntity.ok(service.quote(from, to, amount));
    }
}
