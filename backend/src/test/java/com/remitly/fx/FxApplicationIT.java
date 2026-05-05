package com.remitly.fx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FxApplicationIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void providersAreRegisteredFromConfig() throws Exception {
        mockMvc.perform(get("/api/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void currenciesAreDerivedFromProviderSnapshots() throws Exception {
        mockMvc.perform(get("/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void bestRateAndQuotesEndToEnd() throws Exception {
        mockMvc.perform(get("/api/rates/USD/EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("USD"))
                .andExpect(jsonPath("$.to").value("EUR"))
                .andExpect(jsonPath("$.provider").exists())
                .andExpect(jsonPath("$.rate").isNumber());

        mockMvc.perform(get("/api/rates/USD/EUR/quotes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotes.length()").value(3))
                .andExpect(jsonPath("$.best.provider").exists());
    }

    @Test
    void unknownPairReturns404() throws Exception {
        mockMvc.perform(get("/api/rates/USD/XYZ"))
                .andExpect(status().isNotFound());
    }
}
