package com.chainsea.healthcheck.controller;

import com.chainsea.healthcheck.controller.dto.ServiceStatsResponse;
import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.service.HealthCheckService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
    public ResponseEntity<CollectionModel<EntityModel<HealthCheckRecord>>> getServiceHealthChecks(@PathVariable String serviceName) {
        List<HealthCheckRecord> records = healthCheckService.getHealthChecks(serviceName);
        List<EntityModel<HealthCheckRecord>> entityModels = records.stream()
                .map(healthCheckRecord -> {
                    EntityModel<HealthCheckRecord> entityModel = EntityModel.of(healthCheckRecord);
                    entityModel.add(linkTo(methodOn(HealthCheckController.class).getHealthCheck(healthCheckRecord.getId())).withSelfRel());
                    return entityModel;
                }).toList();

        CollectionModel<EntityModel<HealthCheckRecord>> collectionModel = CollectionModel.of(
                entityModels,
                linkTo(methodOn(ServiceHealthCheckController.class).getServiceHealthChecks(serviceName)).withSelfRel(),
                linkTo(methodOn(ServiceHealthCheckController.class).getServiceStats(serviceName)).withRel("stats"),
                linkTo(methodOn(ServiceHealthCheckController.class).getLatestServiceHealthCheck(serviceName)).withRel("latest")
        );

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * GET /api/services/{serviceName}/health-checks/latest
     * Retrieves the latest health check record for a specific service.
     */
    @GetMapping("/{serviceName}/health-checks/latest")
    public ResponseEntity<EntityModel<HealthCheckRecord>> getLatestServiceHealthCheck(@PathVariable String serviceName) {
        return healthCheckService.getLatestHealthCheck(serviceName)
                .map(healthCheckRecord -> {
                    EntityModel<HealthCheckRecord> entityModel = EntityModel.of(healthCheckRecord);
                    entityModel.add(linkTo(methodOn(HealthCheckController.class).getHealthCheck(healthCheckRecord.getId())).withSelfRel());
                    entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class).getServiceHealthChecks(serviceName)).withRel("all-health-checks"));
                    entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class).getServiceStats(serviceName)).withRel("stats"));
                    return entityModel;
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/services/{serviceName}/stats
     * Retrieves statistics for a specific service.
     */
    @GetMapping("/{serviceName}/stats")
    public ResponseEntity<EntityModel<ServiceStatsResponse>> getServiceStats(@PathVariable String serviceName) {
        long failureCount = healthCheckService.getFailureCount(serviceName);
        Optional<HealthCheckRecord> latest = healthCheckService.getLatestHealthCheck(serviceName);

        ServiceStatsResponse stats = new ServiceStatsResponse(
                serviceName,
                failureCount,
                latest.map(HealthCheckRecord::getStatus).orElse("UNKNOWN"),
                latest.isPresent()
        );

        EntityModel<ServiceStatsResponse> entityModel = EntityModel.of(stats);
        entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class).getServiceStats(serviceName)).withSelfRel());
        entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class).getServiceHealthChecks(serviceName)).withRel("health-checks"));
        entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class).getLatestServiceHealthCheck(serviceName)).withRel("latest"));

        return ResponseEntity.ok(entityModel);
    }
}
