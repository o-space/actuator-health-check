package com.chainsea.healthcheck.health;

import com.chainsea.healthcheck.config.HealthCheckProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component("degradedReadiness")
public class ReadinessHealthIndicator implements HealthIndicator {

    private static final Status DEGRADED = new Status("DEGRADED", "Degraded");

    private final HealthCheckProperties properties;
    private final Map<String, HealthIndicator> healthIndicators;

    public ReadinessHealthIndicator(HealthCheckProperties properties,
                                    PostgresHealthIndicator postgresHealthIndicator,
                                    RedisHealthIndicator redisHealthIndicator,
                                    RabbitMqHealthIndicator rabbitMqHealthIndicator,
                                    MongoDbHealthIndicator mongoDbHealthIndicator,
                                    MockWebServerHealthIndicator mockWebServerHealthIndicator) {
        this.properties = properties;
        this.healthIndicators = new HashMap<>();
        this.healthIndicators.put("postgres", postgresHealthIndicator);
        this.healthIndicators.put("redis", redisHealthIndicator);
        this.healthIndicators.put("rabbitmq", rabbitMqHealthIndicator);
        this.healthIndicators.put("mongodb", mongoDbHealthIndicator);
        this.healthIndicators.put("mockWebServer", mockWebServerHealthIndicator);
    }

    @Override
    public Health health() {
        Map<String, Health> healths = new LinkedHashMap<>();
        Map<String, String> statusSummary = new LinkedHashMap<>();

        for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
            String serviceName = entry.getKey();
            Health serviceHealth;
            try {
                serviceHealth = entry.getValue().health();
            } catch (Exception e) {
                serviceHealth = Health.down()
                        .withException(e)
                        .build();
            }
            healths.put(serviceName, serviceHealth);
            statusSummary.put(serviceName, serviceHealth.getStatus().getCode());
        }

        Set<String> criticalServices = properties.getCriticalServices();
        Set<String> nonCriticalServices = properties.getNonCriticalServices();

        boolean hasCriticalFailure = false;
        boolean hasNonCriticalFailure = false;
        int criticalUpCount = 0;
        int criticalTotalCount = 0;
        int nonCriticalUpCount = 0;
        int nonCriticalTotalCount = 0;

        for (Map.Entry<String, Health> entry : healths.entrySet()) {
            String serviceName = entry.getKey();
            Health health = entry.getValue();
            boolean isUp = Status.UP.equals(health.getStatus());

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

        Health.Builder builder = new Health.Builder();

        if (hasCriticalFailure) {
            builder.status(Status.DOWN)
                    .withDetail("reason", "Critical services are down")
                    .withDetail("criticalServicesUp", criticalUpCount + "/" + criticalTotalCount)
                    .withDetail("nonCriticalServicesUp", nonCriticalUpCount + "/" + nonCriticalTotalCount);
        } else if (hasNonCriticalFailure) {
            builder.status(DEGRADED)
                    .withDetail("reason", "Non-critical services are down, but system is partially available")
                    .withDetail("criticalServicesUp", criticalUpCount + "/" + criticalTotalCount)
                    .withDetail("nonCriticalServicesUp", nonCriticalUpCount + "/" + nonCriticalTotalCount);
        } else {
            builder.status(Status.UP)
                    .withDetail("reason", "All services are up")
                    .withDetail("criticalServicesUp", criticalUpCount + "/" + criticalTotalCount)
                    .withDetail("nonCriticalServicesUp", nonCriticalUpCount + "/" + nonCriticalTotalCount);
        }

        builder.withDetail("services", statusSummary);
        // builder.withDetail("details", healths);

        return builder.build();
    }
}

