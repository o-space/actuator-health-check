package com.chainsea.healthcheck.config;

import com.chainsea.healthcheck.health.HealthStatusCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

public class ServiceHealthCheckScheduler implements InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthCheckScheduler.class);

    private final String serviceName;
    private final HealthIndicator healthIndicator;
    private final HealthStatusCache healthStatusCache;
    private final long interval;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;
    private volatile boolean initialized = false;

    public ServiceHealthCheckScheduler(String serviceName,
                                       HealthIndicator healthIndicator,
                                       HealthStatusCache healthStatusCache,
                                       long interval,
                                       TaskScheduler taskScheduler) {
        this.serviceName = serviceName;
        this.healthIndicator = healthIndicator;
        this.healthStatusCache = healthStatusCache;
        this.interval = interval;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void start() {
        initialize();
    }

    @Override
    public void afterPropertiesSet() {
        initialize();
    }

    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        logger.info("Starting health check scheduler for service: {} with interval: {}ms", serviceName, interval);

        checkService();

        scheduledTask = taskScheduler.scheduleWithFixedDelay(
                this::checkService,
                Duration.ofMillis(interval)
        );
    }

    @PreDestroy
    public void stop() {
        destroy();
    }

    @Override
    public void destroy() {
        if (scheduledTask != null && !scheduledTask.isCancelled()) {
            scheduledTask.cancel(false);
            logger.info("Stopped health check scheduler for service: {}", serviceName);
        }
    }

    private void checkService() {
        logger.debug("Starting health check for service: {}", serviceName);

        try {
            Health health = healthIndicator.health();
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

    public String getServiceName() {
        return serviceName;
    }
}

