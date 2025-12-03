package com.chainsea.healthcheck.config;

import com.chainsea.healthcheck.health.HealthStatusCache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Configuration
public class HealthCheckSchedulerConfig {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckSchedulerConfig.class);

    private final ApplicationContext applicationContext;
    private final HealthCheckProperties properties;
    private final HealthStatusCache healthStatusCache;
    private final TaskScheduler taskScheduler;
    private final List<ServiceHealthCheckScheduler> schedulers = new ArrayList<>();

    public HealthCheckSchedulerConfig(ApplicationContext applicationContext,
                                      HealthCheckProperties properties,
                                      HealthStatusCache healthStatusCache,
                                      TaskScheduler healthCheckTaskScheduler) {
        this.applicationContext = applicationContext;
        this.properties = properties;
        this.healthStatusCache = healthStatusCache;
        this.taskScheduler = healthCheckTaskScheduler;
    }

    @PostConstruct
    public void createSchedulers() {
        Set<String> allServiceNames = properties.getCriticalServiceNames();
        allServiceNames.addAll(properties.getNonCriticalServiceNames());

        for (String serviceName : allServiceNames) {
            try {
                HealthIndicator healthIndicator = applicationContext.getBean(serviceName, HealthIndicator.class);
                long interval = properties.getServiceInterval(serviceName);

                ServiceHealthCheckScheduler scheduler = new ServiceHealthCheckScheduler(
                        serviceName,
                        healthIndicator,
                        healthStatusCache,
                        interval,
                        taskScheduler
                );

                schedulers.add(scheduler);
                scheduler.afterPropertiesSet();
                logger.info("Created health check scheduler for service: {} with interval: {}ms", serviceName, interval);
            } catch (NoSuchBeanDefinitionException e) {
                logger.warn("HealthIndicator bean '{}' not found, skipping scheduler creation", serviceName);
            }
        }
    }

    @PreDestroy
    public void destroySchedulers() {
        for (ServiceHealthCheckScheduler scheduler : schedulers) {
            try {
                scheduler.destroy();
            } catch (Exception e) {
                logger.warn("Error destroying scheduler for service: {}", scheduler.getServiceName(), e);
            }
        }
    }
}
