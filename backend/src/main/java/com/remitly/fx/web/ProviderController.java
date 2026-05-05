package com.remitly.fx.web;

import com.remitly.fx.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ExchangeRateService service;

    public ProviderController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping
    public List<String> list() {
        return service.listProviders();
    }
}
