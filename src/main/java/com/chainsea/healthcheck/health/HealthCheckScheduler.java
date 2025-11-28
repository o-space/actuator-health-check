package com.chainsea.healthcheck.health;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HealthCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final HealthStatusCache healthStatusCache;
    private final Map<String, HealthIndicator> healthIndicators;

    public HealthCheckScheduler(HealthStatusCache healthStatusCache,
                                PostgresHealthIndicator postgresHealthIndicator,
                                RedisHealthIndicator redisHealthIndicator,
                                RabbitMqHealthIndicator rabbitMqHealthIndicator,
                                MongoDbHealthIndicator mongoDbHealthIndicator,
                                MockWebServerHealthIndicator mockWebServerHealthIndicator) {
        this.healthStatusCache = healthStatusCache;
        this.healthIndicators = Map.of(
                "postgres", postgresHealthIndicator,
                "redis", redisHealthIndicator,
                "rabbitmq", rabbitMqHealthIndicator,
                "mongodb", mongoDbHealthIndicator,
                "mockWebServer", mockWebServerHealthIndicator
        );
    }

    @PostConstruct
    public void initialHealthCheck() {
        logger.info("Performing initial health check for all services");
        checkAllServices();
    }

    @Scheduled(fixedDelayString = "${health-check.schedule-interval:5000}")
    public void checkAllServices() {
        logger.debug("Starting scheduled health check for all services");

        for (Map.Entry<String, HealthIndicator> entry : healthIndicators.entrySet()) {
            String serviceName = entry.getKey();
            HealthIndicator indicator = entry.getValue();

            try {
                Health health = indicator.health();
                healthStatusCache.updateHealth(serviceName, health);
                logger.debug("Service {} health check completed: {}", serviceName, health.getStatus());
            } catch (Exception e) {
                logger.warn("Health check failed for service {}: {}", serviceName, e.getMessage());
                Health downHealth = Health.down()
                        .withException(e)
                        .build();
                healthStatusCache.updateHealth(serviceName, downHealth);
            }
        }

        logger.debug("Scheduled health check completed");
    }
}

