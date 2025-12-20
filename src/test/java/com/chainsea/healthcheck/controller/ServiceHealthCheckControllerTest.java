package com.chainsea.healthcheck.controller;

import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.service.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceHealthCheckController.class)
class ServiceHealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Test
    void shouldGetServiceHealthChecks() throws Exception {
        // Given
        String serviceName = "test-service";
        List<HealthCheckRecord> history = List.of(
                new HealthCheckRecord(serviceName, "UP", Map.of("status", "OK"), 100L),
                new HealthCheckRecord(serviceName, "DOWN", Map.of("status", "Error"), 200L)
        );
        when(healthCheckService.getHealthChecks(serviceName)).thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/services/{serviceName}/health-checks", serviceName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].serviceName").value(serviceName))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[1].status").value("DOWN"));
    }

    @Test
    void shouldGetLatestServiceHealthCheck() throws Exception {
        // Given
        String serviceName = "test-service";
        HealthCheckRecord latest = new HealthCheckRecord(serviceName, "UP", Map.of("message", "OK"), 100L);
        when(healthCheckService.getLatestHealthCheck(serviceName)).thenReturn(Optional.of(latest));

        // When & Then
        mockMvc.perform(get("/api/services/{serviceName}/health-checks/latest", serviceName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldReturnNotFoundWhenNoLatestServiceHealthCheck() throws Exception {
        // Given
        String serviceName = "non-existent-service";
        when(healthCheckService.getLatestHealthCheck(serviceName)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/services/{serviceName}/health-checks/latest", serviceName))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetServiceStats() throws Exception {
        // Given
        String serviceName = "test-service";
        HealthCheckRecord latest = new HealthCheckRecord(serviceName, "UP", Map.of("message", "OK"), 100L);
        when(healthCheckService.getFailureCount(serviceName)).thenReturn(5L);
        when(healthCheckService.getLatestHealthCheck(serviceName)).thenReturn(Optional.of(latest));

        // When & Then
        mockMvc.perform(get("/api/services/{serviceName}/stats", serviceName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value(serviceName))
                .andExpect(jsonPath("$.failureCount").value(5))
                .andExpect(jsonPath("$.latestStatus").value("UP"))
                .andExpect(jsonPath("$.hasRecords").value(true));
    }
}
