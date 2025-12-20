package com.chainsea.healthcheck;

import com.chainsea.healthcheck.repository.HealthCheckRecordRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.flyway.enabled=false",
        "health-check.scheduler.enabled=false"
})
class SmokeTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HealthCheckRecordRepository repository;

    @Test
    void shouldLoadApplicationContextGivenSpringBootApplicationWhenStarting() {
        // Given - application context should be loaded
        // When - test runs
        // Then - if we reach here, context loaded successfully
        assertThat(restTemplate).isNotNull();
        assertThat(repository).isNotNull();
    }

    @Test
    void shouldReturnOkGivenActuatorHealthEndpointWhenGettingHealth() {
        // Given - application is running
        String url = "http://localhost:" + port + "/actuator/health";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnOkGivenActuatorLivenessEndpointWhenGettingLiveness() {
        // Given - application is running
        String url = "http://localhost:" + port + "/actuator/health/liveness";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnOkOrServiceUnavailableGivenActuatorReadinessEndpointWhenGettingReadiness() {
        // Given - application is running
        String url = "http://localhost:" + port + "/actuator/health/readiness";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Then
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnOkGivenHealthChecksListEndpointWhenGettingHealthChecks() {
        // Given - application is running
        String url = "http://localhost:" + port + "/api/health-checks";

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldReturnNonNegativeCountGivenRepositoryWhenCountingRecords() {
        // Given - repository is available
        // When - we try to count records
        long count = repository.count();

        // Then - operation should succeed (count can be 0 or more)
        assertThat(count).isNotNegative();
    }
}
