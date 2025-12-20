package com.chainsea.healthcheck.service;

import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.repository.HealthCheckRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class HealthCheckServiceTest {

    @Mock
    private HealthCheckRecordRepository repository;

    @Mock
    private RestClient restClient;

    @Mock
    private RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private ResponseSpec responseSpec;

    @InjectMocks
    private HealthCheckServiceImpl healthCheckService;

    @BeforeEach
    void setUp() {
        // Use lenient() for stubbing that are only used in some tests
        lenient().when(restClient.get()).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.uri(any(java.net.URI.class))).thenReturn(requestHeadersUriSpec);
        lenient().when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void shouldReturnUpStatusGivenSuccessfulResponseWhenCheckingHealth() throws Exception {
        // Given
        String serviceName = "test-service";
        URL url = URI.create("http://example.com/health").toURL();
        ResponseEntity<String> response = new ResponseEntity<>("OK", HttpStatus.OK);

        when(responseSpec.toEntity(String.class)).thenReturn(response);

        Map<String, Object> details = Map.of("message", "Health check successful", "responseBody", "OK");
        HealthCheckRecord savedRecord = new HealthCheckRecord(serviceName, "UP", details, 100L);
        when(repository.save(any(HealthCheckRecord.class))).thenReturn(savedRecord);

        // When
        HealthCheckRecord result = healthCheckService.check(serviceName, url);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getServiceName()).isEqualTo(serviceName);
        assertThat(result.getStatus()).isEqualTo("UP");
        verify(repository, times(1)).save(any(HealthCheckRecord.class));
        verify(restClient, times(1)).get();
    }

    @Test
    void shouldReturnDownStatusGivenExceptionWhenCheckingHealth() throws Exception {
        // Given
        String serviceName = "test-service";
        URL url = URI.create("http://example.com/health").toURL();
        RuntimeException exception = new RuntimeException("Connection timeout");

        when(responseSpec.toEntity(String.class)).thenThrow(exception);

        Map<String, Object> details = Map.of("message", "Health check failed", "error", "Connection timeout");
        HealthCheckRecord savedRecord = new HealthCheckRecord(serviceName, "DOWN", details, 50L);
        when(repository.save(any(HealthCheckRecord.class))).thenReturn(savedRecord);

        // When
        HealthCheckRecord result = healthCheckService.check(serviceName, url);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DOWN");
        assertThat(result.getDetails()).containsEntry("message", "Health check failed");
        verify(repository, times(1)).save(any(HealthCheckRecord.class));
    }

    @Test
    void shouldReturnDegradedStatusGivenNonOkResponseWhenCheckingHealth() throws Exception {
        // Given
        String serviceName = "test-service";
        URL url = URI.create("http://example.com/health").toURL();
        ResponseEntity<String> response = new ResponseEntity<>("Error", HttpStatus.INTERNAL_SERVER_ERROR);

        when(responseSpec.toEntity(String.class)).thenReturn(response);

        ArgumentCaptor<HealthCheckRecord> recordCaptor = ArgumentCaptor.forClass(HealthCheckRecord.class);
        Map<String, Object> details = Map.of("message", "Health check returned non-OK status", "statusCode", 500);
        HealthCheckRecord savedRecord = new HealthCheckRecord(serviceName, "DEGRADED", details, 100L);
        when(repository.save(recordCaptor.capture())).thenReturn(savedRecord);

        // When
        HealthCheckRecord result = healthCheckService.check(serviceName, url);

        // Then
        assertThat(result).isNotNull();
        assertThat(recordCaptor.getValue().getStatus()).isEqualTo("DEGRADED");
        assertThat(recordCaptor.getValue().getDetails()).containsEntry("statusCode", 500);
        verify(repository, times(1)).save(any(HealthCheckRecord.class));
    }

    @Test
    void shouldReturnHealthCheckHistoryGivenServiceNameWhenGettingHealthChecks() {
        // Given
        String serviceName = "test-service";
        List<HealthCheckRecord> history = List.of(
                new HealthCheckRecord(serviceName, "UP", Map.of("message", "OK"), 100L),
                new HealthCheckRecord(serviceName, "DOWN", Map.of("message", "Error"), 200L)
        );
        when(repository.findByServiceNameOrderByCheckedAtDesc(serviceName)).thenReturn(history);

        // When
        List<HealthCheckRecord> result = healthCheckService.getHealthChecks(serviceName);

        // Then
        assertThat(result).hasSize(2);
        verify(repository, times(1)).findByServiceNameOrderByCheckedAtDesc(serviceName);
    }

    @Test
    void shouldReturnLatestHealthCheckGivenServiceNameWhenGettingLatestHealthCheck() {
        // Given
        String serviceName = "test-service";
        HealthCheckRecord latest = new HealthCheckRecord(serviceName, "UP", Map.of("status", "OK"), 100L);
        when(repository.findFirstByServiceNameOrderByCheckedAtDesc(serviceName)).thenReturn(Optional.of(latest));

        // When
        Optional<HealthCheckRecord> result = healthCheckService.getLatestHealthCheck(serviceName);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo("UP");
        verify(repository, times(1)).findFirstByServiceNameOrderByCheckedAtDesc(serviceName);
    }

    @Test
    void shouldReturnRecentHealthChecksGivenHoursWhenGettingRecentHealthChecks() {
        // Given
        int hours = 24;
        List<HealthCheckRecord> recent = List.of(
                new HealthCheckRecord("service1", "UP", Map.of("message", "OK"), 100L),
                new HealthCheckRecord("service2", "DOWN", Map.of("message", "Error"), 200L)
        );
        when(repository.findRecentRecords(any(LocalDateTime.class))).thenReturn(recent);

        // When
        List<HealthCheckRecord> result = healthCheckService.getHealthChecks(hours);

        // Then
        assertThat(result).hasSize(2);
        verify(repository, times(1)).findRecentRecords(any(LocalDateTime.class));
    }

    @Test
    void shouldReturnFailureCountGivenServiceNameWhenGettingFailureCount() {
        // Given
        String serviceName = "test-service";
        long failureCount = 5L;
        when(repository.countByServiceNameAndStatus(serviceName, "DOWN")).thenReturn(failureCount);

        // When
        long result = healthCheckService.getFailureCount(serviceName);

        // Then
        assertThat(result).isEqualTo(5L);
        verify(repository, times(1)).countByServiceNameAndStatus(serviceName, "DOWN");
    }
}
