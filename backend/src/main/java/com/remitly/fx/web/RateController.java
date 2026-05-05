package com.remitly.fx.web;

import com.remitly.fx.model.RateComparison;
import com.remitly.fx.model.RateQuote;
import com.remitly.fx.service.ExchangeRateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rates")
public class RateController {

    private final ExchangeRateService service;

    public RateController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping
    public List<RateQuote> listBest() {
        return service.listBestRates();
    }

    @GetMapping("/{from}/{to}")
    public RateQuote best(@PathVariable String from, @PathVariable String to) {
        return service.getBestRate(from, to);
    }

    @GetMapping("/{from}/{to}/quotes")
    public RateComparison quotes(@PathVariable String from, @PathVariable String to) {
        return service.compare(from, to);
    }

    @PostMapping("/refresh")
    public List<RateQuote> refresh() {
        service.refreshNow();
        return service.listBestRates();
    }
}
