package com.chainsea.healthcheck.health;

import com.chainsea.healthcheck.config.ConditionalOnServiceConfigured;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component("mockWebServer")
@ConditionalOnServiceConfigured("mockWebServer")
public class MockWebServerHealthIndicator extends AbstractHealthIndicator {

    private final RestClient restClient;
    private final String mockServerUrl;

    public MockWebServerHealthIndicator(RestClient restClient,
                                        @Value("${monitoring.mock-server-url}") String mockServerUrl) {
        this.restClient = restClient;
        this.mockServerUrl = mockServerUrl;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            var response = restClient.get()
                    .uri(mockServerUrl)
                    .retrieve()
                    .toEntity(String.class);

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                builder.up();
            } else {
                builder.down().withDetail("status", response.getStatusCode().value());
            }
        } catch (Exception ex) {
            builder.down(ex);
        }
    }
}

