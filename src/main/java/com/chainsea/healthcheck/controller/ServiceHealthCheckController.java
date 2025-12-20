package com.chainsea.healthcheck.controller;

import com.chainsea.healthcheck.controller.dto.ServiceStatsResponse;
import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.service.HealthCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/services")
public class ServiceHealthCheckController {

    private final HealthCheckService healthCheckService;

    public ServiceHealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    /**
     * GET /api/services/{serviceName}/health-checks
     * Retrieves all health check records for a specific service.
     */
    @GetMapping("/{serviceName}/health-checks")
    public ResponseEntity<List<HealthCheckRecord>> getServiceHealthChecks(@PathVariable String serviceName) {
        return ResponseEntity.ok(healthCheckService.getHealthChecks(serviceName));
    }

    /**
     * GET /api/services/{serviceName}/health-checks/latest
     * Retrieves the latest health check record for a specific service.
     */
    @GetMapping("/{serviceName}/health-checks/latest")
    public ResponseEntity<HealthCheckRecord> getLatestServiceHealthCheck(@PathVariable String serviceName) {
        return healthCheckService.getLatestHealthCheck(serviceName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/services/{serviceName}/stats
     * Retrieves statistics for a specific service.
     */
    @GetMapping("/{serviceName}/stats")
    public ResponseEntity<ServiceStatsResponse> getServiceStats(@PathVariable String serviceName) {
        long failureCount = healthCheckService.getFailureCount(serviceName);
        Optional<HealthCheckRecord> latest = healthCheckService.getLatestHealthCheck(serviceName);

        ServiceStatsResponse stats = new ServiceStatsResponse(
                serviceName,
                failureCount,
                latest.map(HealthCheckRecord::getStatus).orElse("UNKNOWN"),
                latest.isPresent()
        );
        return ResponseEntity.ok(stats);
    }
}
