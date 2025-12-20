package com.chainsea.healthcheck.controller;

import com.chainsea.healthcheck.controller.dto.HealthCheckRequest;
import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.service.HealthCheckService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthCheckController.class)
class HealthCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Test
    void shouldPerformHealthCheck() throws Exception {
        // Given
        URL url = URI.create("http://example.com/health").toURL();
        HealthCheckRequest request = new HealthCheckRequest("test-service", url);
        Map<String, Object> details = Map.of("message", "OK");
        HealthCheckRecord healthCheckRecord = new HealthCheckRecord("test-service", "UP", details, 100L);
        when(healthCheckService.check("test-service", url)).thenReturn(healthCheckRecord);

        // When & Then
        mockMvc.perform(post("/api/health-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.responseTimeMs").value(100));
    }

    @Test
    void shouldReturnBadRequestWhenMissingParameters() throws Exception {
        // Given - missing url field
        String requestJson = "{\"serviceName\":\"test-service\"}";

        // When & Then
        mockMvc.perform(post("/api/health-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenServiceNameIsBlank() throws Exception {
        // Given - blank serviceName
        URL url = URI.create("http://example.com/health").toURL();
        HealthCheckRequest request = new HealthCheckRequest("", url);

        // When & Then
        mockMvc.perform(post("/api/health-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsNull() throws Exception {
        // Given - null url (validation will fail)
        HealthCheckRequest request = new HealthCheckRequest("test-service", null);

        // When & Then
        mockMvc.perform(post("/api/health-checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetHealthCheckById() throws Exception {
        // Given
        Long id = 1L;
        Map<String, Object> details = Map.of("message", "OK");
        HealthCheckRecord healthCheckRecord = new HealthCheckRecord("test-service", "UP", details, 100L);
        healthCheckRecord.setId(id);
        when(healthCheckService.getHealthCheckById(id)).thenReturn(Optional.of(healthCheckRecord));

        // When & Then
        mockMvc.perform(get("/api/health-checks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.serviceName").value("test-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldReturnNotFoundWhenHealthCheckNotFound() throws Exception {
        // Given
        Long id = 999L;
        when(healthCheckService.getHealthCheckById(id)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/health-checks/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetHealthChecksWithServiceNameFilter() throws Exception {
        // Given
        String serviceName = "test-service";
        int hours = 24;
        List<HealthCheckRecord> history = List.of(
                new HealthCheckRecord(serviceName, "UP", Map.of("status", "OK"), 100L),
                new HealthCheckRecord(serviceName, "DOWN", Map.of("status", "Error"), 200L)
        );
        when(healthCheckService.getHealthChecks(serviceName, hours)).thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/health-checks")
                        .param("serviceName", serviceName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].serviceName").value(serviceName))
                .andExpect(jsonPath("$[0].status").value("UP"))
                .andExpect(jsonPath("$[1].status").value("DOWN"));
    }

    @Test
    void shouldGetHealthChecksWithServiceNameAndHoursFilter() throws Exception {
        // Given
        String serviceName = "test-service";
        int hours = 12;
        List<HealthCheckRecord> history = List.of(new HealthCheckRecord(serviceName, "UP", Map.of("status", "OK"), 100L));
        when(healthCheckService.getHealthChecks(serviceName, hours)).thenReturn(history);

        // When & Then
        mockMvc.perform(get("/api/health-checks")
                        .param("serviceName", serviceName)
                        .param("hours", String.valueOf(hours)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].serviceName").value(serviceName))
                .andExpect(jsonPath("$[0].status").value("UP"));
    }

    @Test
    void shouldGetHealthChecksWithHoursFilter() throws Exception {
        // Given
        List<HealthCheckRecord> recent = List.of(
                new HealthCheckRecord("service1", "UP", Map.of("message", "OK"), 100L),
                new HealthCheckRecord("service2", "DOWN", Map.of("message", "Error"), 200L)
        );
        when(healthCheckService.getHealthChecks(24)).thenReturn(recent);

        // When & Then
        mockMvc.perform(get("/api/health-checks")
                        .param("hours", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].serviceName").value("service1"))
                .andExpect(jsonPath("$[1].serviceName").value("service2"));
    }

    @Test
    void shouldGetHealthChecksWithDefaultHours() throws Exception {
        // Given
        List<HealthCheckRecord> recent = List.of();
        when(healthCheckService.getHealthChecks(24)).thenReturn(recent);

        // When & Then
        mockMvc.perform(get("/api/health-checks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
