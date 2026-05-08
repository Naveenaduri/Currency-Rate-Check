package com.remitly.fx.config;

import com.remitly.fx.wise.WiseComparisonClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderConfigTest {

    @Test
    void buildsAWiseClientWithConfiguredBaseUrl() {
        ProviderConfig config = new ProviderConfig();
        WiseComparisonClient client = config.wiseComparisonClient(
                "https://example.test", 1000, 2000);
        assertThat(client).isNotNull();
    }
}
