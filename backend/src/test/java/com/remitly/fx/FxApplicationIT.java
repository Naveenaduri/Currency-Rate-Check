package com.remitly.fx;

import com.remitly.fx.wise.WiseComparisonClient;
import com.remitly.fx.wise.WiseComparisonResponse;
import com.remitly.fx.wise.WiseComparisonResponse.DeliveryEstimation;
import com.remitly.fx.wise.WiseComparisonResponse.Provider;
import com.remitly.fx.wise.WiseComparisonResponse.Quote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FxApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WiseComparisonClient wise;

    @BeforeEach
    void setUp() {
        Quote remitlyQ = new Quote(
                new BigDecimal("94.30"), BigDecimal.ZERO, new BigDecimal("0.27"),
                new BigDecimal("94300"), Instant.parse("2026-05-08T00:00:00Z"),
                "US", "IN",
                new DeliveryEstimation(null, null, null, true));
        Quote wiseQ = new Quote(
                new BigDecimal("94.50"), new BigDecimal("2.00"), BigDecimal.ZERO,
                new BigDecimal("94405"), Instant.parse("2026-05-08T00:00:00Z"),
                "US", "IN",
                new DeliveryEstimation(null, BigDecimal.ONE, "hours", true));
        Quote chaseQ = new Quote(
                new BigDecimal("91.95"), new BigDecimal("10.00"), new BigDecimal("3.20"),
                new BigDecimal("91950"), Instant.parse("2026-05-08T00:00:00Z"),
                "US", "IN",
                new DeliveryEstimation(null, null, null, true));
        WiseComparisonResponse response = new WiseComparisonResponse(List.of(
                new Provider(104, "remitly", "Remitly", "https://logo/remitly.svg",
                        "moneyTransferProvider", false, List.of(remitlyQ)),
                new Provider(1, "wise", "Wise", "https://logo/wise.svg",
                        "moneyTransferProvider", true, List.of(wiseQ)),
                new Provider(20, "chase", "Chase", "https://logo/chase.svg",
                        "bank", false, List.of(chaseQ))
        ));
        when(wise.fetchComparison(any(), any(), any())).thenReturn(response);
    }

    @Test
    void currenciesEndpointReturnsConfiguredList() throws Exception {
        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").value("AUD"));
    }

    @Test
    void quoteEndpointEndToEnd() throws Exception {
        mockMvc.perform(get("/api/quotes")
                        .param("from", "USD").param("to", "INR").param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("INR"))
                .andExpect(jsonPath("$.providers.length()").value(3))
                .andExpect(jsonPath("$.providers[0].bestDeal").value(true))
                .andExpect(jsonPath("$.baselineName").value("Chase"));
    }

    @Test
    void providersEndpointPopulatesAfterFirstQuote() throws Exception {
        mockMvc.perform(get("/api/quotes")
                .param("from", "USD").param("to", "INR").param("amount", "1000"));

        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
