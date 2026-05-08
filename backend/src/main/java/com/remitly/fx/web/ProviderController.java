package com.remitly.fx.web;

import com.remitly.fx.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ExchangeRateService service;

    public ProviderController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping
    public Set<String> list() {
        return service.listProviders();
    }
}
