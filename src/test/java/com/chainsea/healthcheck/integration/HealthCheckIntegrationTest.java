package com.chainsea.healthcheck.integration;

import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.repository.HealthCheckRecordRepository;
import com.chainsea.healthcheck.service.HealthCheckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.jpa.show-sql=true",
        "spring.flyway.enabled=false"
})
class HealthCheckIntegrationTest {

    @Container
    static GenericContainer<?> wiremockContainer = new GenericContainer<>(
            DockerImageName.parse("wiremock/wiremock:3.13.2"))
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp("/__admin").forStatusCode(200).withStartupTimeout(java.time.Duration.ofSeconds(60)));

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private HealthCheckRecordRepository repository;

    @Autowired
    private RestClient restClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Container is automatically started by Testcontainers before this method is called
        String mockServerUrl = "http://localhost:" + wiremockContainer.getMappedPort(8080) + "/health";
        registry.add("monitoring.mock-server-url", () -> mockServerUrl);
    }

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        // Create a stub for /health endpoint in WireMock
        createHealthEndpointStub();
    }

    private void createHealthEndpointStub() {
        String wiremockBaseUrl = "http://localhost:" + wiremockContainer.getMappedPort(8080);
        Map<String, Object> stubMapping = Map.of(
                "request", Map.of(
                        "method", "GET",
                        "urlPath", "/health"
                ),
                "response", Map.of(
                        "status", 200,
                        "body", "{\"status\":\"ok\"}",
                        "headers", Map.of("Content-Type", "application/json")
                )
        );

        try {
            restClient.post()
                    .uri(wiremockBaseUrl + "/__admin/mappings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(stubMapping)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Ignore if stub creation fails, tests will handle it
        }
    }

    @Test
    void shouldPerformHealthCheckWithRealExternalApi() throws Exception {
        // Given
        String serviceName = "wiremock-service";
        URL healthCheckUrl = URI.create("http://localhost:" + wiremockContainer.getMappedPort(8080) + "/health").toURL();

        // When
        HealthCheckRecord healthCheckRecord = healthCheckService.check(serviceName, healthCheckUrl);

        // Then
        assertThat(healthCheckRecord).isNotNull();
        assertThat(healthCheckRecord.getServiceName()).isEqualTo(serviceName);
        assertThat(healthCheckRecord.getStatus()).isIn("UP", "DEGRADED", "DOWN");
        assertThat(healthCheckRecord.getResponseTimeMs()).isPositive();

        // Verify it was saved to database
        Optional<HealthCheckRecord> saved = repository.findById(healthCheckRecord.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getServiceName()).isEqualTo(serviceName);
    }

    @Test
    void shouldHandleFailedExternalApiCall() throws Exception {
        // Given
        String serviceName = "non-existent-service";
        URL invalidUrl = URI.create("http://localhost:" + wiremockContainer.getMappedPort(8080) + "/non-existent").toURL();

        // When
        HealthCheckRecord healthCheckRecord = healthCheckService.check(serviceName, invalidUrl);

        // Then
        assertThat(healthCheckRecord).isNotNull();
        assertThat(healthCheckRecord.getStatus()).isEqualTo("DOWN");
        assertThat(healthCheckRecord.getDetails()).isNotNull();

        // Verify it was saved to database
        Optional<HealthCheckRecord> saved = repository.findById(healthCheckRecord.getId());
        assertThat(saved).isPresent();
    }

    @Test
    void shouldSaveAndRetrieveHealthCheckHistory() throws Exception {
        // Given
        String serviceName = "test-service";
        URL healthCheckUrl = URI.create("http://localhost:" + wiremockContainer.getMappedPort(8080) + "/health").toURL();

        // When - perform multiple health checks
        healthCheckService.check(serviceName, healthCheckUrl);
        healthCheckService.check(serviceName, healthCheckUrl);
        healthCheckService.check(serviceName, healthCheckUrl);

        // Then - verify all records are saved
        List<HealthCheckRecord> history = healthCheckService.getHealthChecks(serviceName);
        assertThat(history).hasSize(3);
        assertThat(history.get(0).getServiceName()).isEqualTo(serviceName);
    }

    @Test
    void shouldGetLatestHealthCheckFromDatabase() throws Exception {
        // Given
        String serviceName = "test-service";
        URL healthCheckUrl = URI.create("http://localhost:" + wiremockContainer.getMappedPort(8080) + "/health").toURL();

        // When
        healthCheckService.check(serviceName, healthCheckUrl);
        healthCheckService.check(serviceName, healthCheckUrl);
        HealthCheckRecord latest = healthCheckService.check(serviceName, healthCheckUrl);

        // Then
        Optional<HealthCheckRecord> retrieved = healthCheckService.getLatestHealthCheck(serviceName);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isEqualTo(latest.getId());
    }

    @Test
    void shouldCountFailuresInDatabase() {
        // Given
        String serviceName = "test-service";
        Map<String, Object> details1 = Map.of("message", "OK");
        Map<String, Object> details2 = Map.of("message", "Error");
        repository.save(new HealthCheckRecord(serviceName, "UP", details1, 100L));
        repository.save(new HealthCheckRecord(serviceName, "DOWN", details2, 200L));
        repository.save(new HealthCheckRecord(serviceName, "DOWN", details2, 300L));

        // When
        long failureCount = healthCheckService.getFailureCount(serviceName);

        // Then
        assertThat(failureCount).isEqualTo(2);
    }

    @Test
    void shouldGetRecentHealthChecksFromDatabase() {
        // Given
        String serviceName = "test-service";
        Map<String, Object> details = Map.of("message", "OK");
        repository.save(new HealthCheckRecord(serviceName, "UP", details, 100L));
        repository.save(new HealthCheckRecord(serviceName, "UP", details, 150L));

        // When
        List<HealthCheckRecord> recent = healthCheckService.getHealthChecks(24);

        // Then
        assertThat(recent).hasSize(2);
    }
}
