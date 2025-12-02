package com.chainsea.healthcheck.health;

import com.chainsea.healthcheck.config.HealthCheckProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component("degradedReadiness")
public class ReadinessHealthIndicator implements HealthIndicator {

    private static final Status DEGRADED = new Status("DEGRADED", "Degraded");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final HealthCheckProperties properties;
    private final HealthStatusCache healthStatusCache;

    public ReadinessHealthIndicator(HealthCheckProperties properties,
                                    HealthStatusCache healthStatusCache) {
        this.properties = properties;
        this.healthStatusCache = healthStatusCache;
    }

    @Override
    public Health health() {
        Map<String, HealthStatusCache.CachedHealth> cachedHealths = healthStatusCache.getAllCachedHealths();
        Map<String, Map<String, String>> servicesInfo = buildServicesInfo(cachedHealths);
        ServiceStatusSummary summary = analyzeServiceStatus(cachedHealths);

        return buildHealthResponse(summary, servicesInfo);
    }

    private Map<String, Map<String, String>> buildServicesInfo(Map<String, HealthStatusCache.CachedHealth> cachedHealths) {
        return cachedHealths.entrySet()
                .stream()
                .map(health -> Map.entry(
                        health.getKey(),
                        Map.of("status", health.getValue().health().getStatus().getCode(),
                                "time", TIME_FORMATTER.format(health.getValue().lastUpdateTime()))))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ServiceStatusSummary analyzeServiceStatus(Map<String, HealthStatusCache.CachedHealth> cachedHealths) {
        Set<String> criticalServices = properties.getCriticalServiceNames();
        Set<String> nonCriticalServices = properties.getNonCriticalServiceNames();

        boolean hasCriticalFailure = false;
        boolean hasNonCriticalFailure = false;
        int criticalUpCount = 0;
        int criticalTotalCount = 0;
        int nonCriticalUpCount = 0;
        int nonCriticalTotalCount = 0;

        for (Map.Entry<String, HealthStatusCache.CachedHealth> entry : cachedHealths.entrySet()) {
            String serviceName = entry.getKey();
            boolean isUp = Status.UP.equals(entry.getValue().health().getStatus());

            if (criticalServices.contains(serviceName)) {
                criticalTotalCount++;
                if (isUp) {
                    criticalUpCount++;
                } else {
                    hasCriticalFailure = true;
                }
            } else if (nonCriticalServices.contains(serviceName)) {
                nonCriticalTotalCount++;
                if (isUp) {
                    nonCriticalUpCount++;
                } else {
                    hasNonCriticalFailure = true;
                }
            }
        }

        return new ServiceStatusSummary(
                hasCriticalFailure,
                hasNonCriticalFailure,
                criticalUpCount,
                criticalTotalCount,
                nonCriticalUpCount,
                nonCriticalTotalCount
        );
    }

    private Health buildHealthResponse(ServiceStatusSummary summary, Map<String, Map<String, String>> servicesInfo) {
        Health.Builder builder = new Health.Builder();

        String criticalServicesUp = summary.criticalUpCount() + "/" + summary.criticalTotalCount();
        String nonCriticalServicesUp = summary.nonCriticalUpCount() + "/" + summary.nonCriticalTotalCount();

        if (summary.hasCriticalFailure()) {
            builder.status(Status.DOWN)
                    .withDetail("reason", "Critical services are down")
                    .withDetail("criticalServicesUp", criticalServicesUp)
                    .withDetail("nonCriticalServicesUp", nonCriticalServicesUp);
        } else if (summary.hasNonCriticalFailure()) {
            builder.status(DEGRADED)
                    .withDetail("reason", "Non-critical services are down, but system is partially available")
                    .withDetail("criticalServicesUp", criticalServicesUp)
                    .withDetail("nonCriticalServicesUp", nonCriticalServicesUp);
        } else {
            builder.status(Status.UP)
                    .withDetail("reason", "All services are up")
                    .withDetail("criticalServicesUp", criticalServicesUp)
                    .withDetail("nonCriticalServicesUp", nonCriticalServicesUp);
        }

        builder.withDetail("services", servicesInfo);
        return builder.build();
    }

    private record ServiceStatusSummary(
            boolean hasCriticalFailure,
            boolean hasNonCriticalFailure,
            int criticalUpCount,
            int criticalTotalCount,
            int nonCriticalUpCount,
            int nonCriticalTotalCount
    ) {
    }
}

