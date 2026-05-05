package com.remitly.fx.web;

import com.remitly.fx.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.TreeSet;

@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    private final ExchangeRateService service;

    public CurrencyController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping
    public List<String> list() {
        return List.copyOf(new TreeSet<>(service.listCurrencies()));
    }
}
