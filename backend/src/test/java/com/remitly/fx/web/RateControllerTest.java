package com.remitly.fx.web;

import com.remitly.fx.model.Quote;
import com.remitly.fx.model.RateComparison;
import com.remitly.fx.model.RateQuote;
import com.remitly.fx.service.ExchangeRateService;
import com.remitly.fx.service.RateNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RateController.class)
class RateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService service;

    @Test
    void listBestRates() throws Exception {
        when(service.listBestRates()).thenReturn(List.of(
                new RateQuote("USD", "EUR", new BigDecimal("0.93"), "B"),
                new RateQuote("USD", "GBP", new BigDecimal("0.79"), "A")
        ));

        mockMvc.perform(get("/api/rates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].provider").value("B"))
                .andExpect(jsonPath("$[0].rate").value(0.93));
    }

    @Test
    void bestRateReturnsProvider() throws Exception {
        when(service.getBestRate("USD", "EUR"))
                .thenReturn(new RateQuote("USD", "EUR", new BigDecimal("0.93"), "B"));

        mockMvc.perform(get("/api/rates/USD/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.rate").value(0.93))
                .andExpect(jsonPath("$.provider").value("B"));
    }

    @Test
    void bestRateReturns404WhenMissing() throws Exception {
        when(service.getBestRate("USD", "XYZ"))
                .thenThrow(new RateNotFoundException("No provider quotes a direct rate for USD -> XYZ"));

        mockMvc.perform(get("/api/rates/USD/XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No provider quotes a direct rate for USD -> XYZ"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void quotesReturnsAllProviders() throws Exception {
        Quote best = new Quote("B", new BigDecimal("0.93"));
        when(service.compare("USD", "EUR"))
                .thenReturn(new RateComparison("USD", "EUR", best, List.of(
                        best,
                        new Quote("A", new BigDecimal("0.91"))
                )));

        mockMvc.perform(get("/api/rates/USD/EUR/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.best.provider").value("B"))
                .andExpect(jsonPath("$.quotes.length()").value(2))
                .andExpect(jsonPath("$.quotes[0].provider").value("B"))
                .andExpect(jsonPath("$.quotes[1].provider").value("A"));
    }

    @Test
    void refreshTriggersServiceAndReturnsBestRates() throws Exception {
        when(service.listBestRates()).thenReturn(List.of(
                new RateQuote("USD", "EUR", new BigDecimal("0.93"), "B")));

        mockMvc.perform(post("/api/rates/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(service).refreshNow();
    }
}
