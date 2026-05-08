package com.remitly.fx.web;

import com.remitly.fx.model.ProviderQuote;
import com.remitly.fx.model.QuoteResponse;
import com.remitly.fx.service.ExchangeRateService;
import com.remitly.fx.service.RateNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuoteController.class)
class QuoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService service;

    private QuoteResponse sampleResponse() {
        return new QuoteResponse(
                "USD", "INR",
                new BigDecimal("1000.00"),
                new BigDecimal("94300.00"),
                "Chase",
                Instant.parse("2026-05-08T03:46:25Z"),
                "Wise V3 Comparisons API",
                List.of(
                        new ProviderQuote(
                                "wise", "Wise", "https://logo/wise.svg", "moneyTransferProvider",
                                new BigDecimal("94.500000"), new BigDecimal("2.00"),
                                BigDecimal.ZERO,
                                new BigDecimal("94405.00"), new BigDecimal("2455.00"),
                                null, null,
                                "2026-05-08T03:46:25Z",
                                true, false),
                        new ProviderQuote(
                                "chase", "Chase", "https://logo/chase.svg", "bank",
                                new BigDecimal("91.950000"), new BigDecimal("10.00"),
                                new BigDecimal("3.20"),
                                new BigDecimal("91950.00"), BigDecimal.ZERO,
                                null, null,
                                "2026-05-08T03:46:25Z",
                                false, true)
                ));
    }

    @Test
    void quoteReturnsProviderList() throws Exception {
        when(service.quote(eq("USD"), eq("INR"), any(BigDecimal.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get("/api/quotes")
                        .param("from", "USD").param("to", "INR").param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.source").value("Wise V3 Comparisons API"))
                .andExpect(jsonPath("$.providers.length()").value(2))
                .andExpect(jsonPath("$.providers[0].id").value("wise"))
                .andExpect(jsonPath("$.providers[0].logoUrl").value("https://logo/wise.svg"))
                .andExpect(jsonPath("$.providers[0].bestDeal").value(true))
                .andExpect(jsonPath("$.providers[1].baseline").value(true))
                .andExpect(jsonPath("$.baselineName").value("Chase"));
    }

    @Test
    void quoteReturns404WhenServiceThrowsNotFound() throws Exception {
        when(service.quote(eq("USD"), eq("XYZ"), any(BigDecimal.class)))
                .thenThrow(new RateNotFoundException("No comparison data for USD -> XYZ"));

        mockMvc.perform(get("/api/quotes")
                        .param("from", "USD").param("to", "XYZ").param("amount", "1000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void quoteReturns400ForNonPositiveAmount() throws Exception {
        when(service.quote(eq("USD"), eq("INR"), any(BigDecimal.class)))
                .thenThrow(new IllegalArgumentException("amount must be positive"));

        mockMvc.perform(get("/api/quotes")
                        .param("from", "USD").param("to", "INR").param("amount", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshInvalidatesCacheAndReturnsFreshQuote() throws Exception {
        when(service.quote(eq("USD"), eq("INR"), any(BigDecimal.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/quotes/refresh")
                        .param("from", "USD").param("to", "INR").param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.length()").value(2));

        verify(service).refresh("USD", "INR");
    }
}
