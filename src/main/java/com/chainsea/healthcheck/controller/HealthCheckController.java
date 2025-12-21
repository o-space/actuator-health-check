package com.chainsea.healthcheck.controller;

import com.chainsea.healthcheck.controller.dto.HealthCheckRequest;
import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.service.HealthCheckService;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/health-checks")
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    /**
     * POST /api/health-checks
     * Creates a new health check by performing a health check for the specified service.
     */
    @PostMapping
    public ResponseEntity<EntityModel<HealthCheckRecord>> check(@Valid @RequestBody HealthCheckRequest request) {
        HealthCheckRecord healthCheckRecord = healthCheckService.check(request.serviceName(), request.url());
        EntityModel<HealthCheckRecord> entityModel = toEntityModel(healthCheckRecord);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(healthCheckRecord.getId())
                .toUri();
        return ResponseEntity.created(location).body(entityModel);
    }

    /**
     * GET /api/health-checks/{id}
     * Retrieves a specific health check record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<HealthCheckRecord>> getHealthCheck(@PathVariable Long id) {
        return healthCheckService.getHealthCheckById(id)
                .map(this::toEntityModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/health-checks
     * Retrieves health check records with optional filtering.
     * Query parameters:
     * - serviceName: filter by service name (optional)
     * - hours: filter by time window (default: 24, applies to all queries)
     * <p>
     * If serviceName is provided, returns records for that service within the time window.
     * If serviceName is not provided, returns all records within the time window.
     */
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<HealthCheckRecord>>> getHealthChecks(
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "24") int hours) {
        List<HealthCheckRecord> records;
        if (serviceName != null && !serviceName.isBlank()) {
            records = healthCheckService.getHealthChecks(serviceName, hours);
        } else {
            records = healthCheckService.getHealthChecks(hours);
        }

        List<EntityModel<HealthCheckRecord>> entityModels = records.stream()
                .map(this::toEntityModel)
                .collect(Collectors.toList());

        CollectionModel<EntityModel<HealthCheckRecord>> collectionModel = CollectionModel.of(
                entityModels,
                linkTo(methodOn(HealthCheckController.class).getHealthChecks(serviceName, hours)).withSelfRel()
        );

        return ResponseEntity.ok(collectionModel);
    }

    /**
     * Converts HealthCheckRecord to EntityModel with HATEOAS links.
     */
    private EntityModel<HealthCheckRecord> toEntityModel(HealthCheckRecord healthCheckRecord) {
        EntityModel<HealthCheckRecord> entityModel = EntityModel.of(healthCheckRecord);
        entityModel.add(linkTo(methodOn(HealthCheckController.class).getHealthCheck(healthCheckRecord.getId())).withSelfRel());
        entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class)
                .getServiceHealthChecks(healthCheckRecord.getServiceName())).withRel("service-health-checks"));
        entityModel.add(linkTo(methodOn(ServiceHealthCheckController.class)
                .getServiceStats(healthCheckRecord.getServiceName())).withRel("service-stats"));
        return entityModel;
    }
}
