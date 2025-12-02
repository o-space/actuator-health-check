package com.chainsea.healthcheck.config;

import com.chainsea.healthcheck.health.HealthStatusCache;
import com.chainsea.healthcheck.health.MockWebServerHealthIndicator;
import com.chainsea.healthcheck.health.MongoDbHealthIndicator;
import com.chainsea.healthcheck.health.PostgresHealthIndicator;
import com.chainsea.healthcheck.health.RabbitMqHealthIndicator;
import com.chainsea.healthcheck.health.RedisHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class HealthCheckSchedulerConfig {

    @Bean
    public TaskScheduler healthCheckTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("health-check-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public ServiceHealthCheckScheduler postgresHealthCheckScheduler(
            HealthStatusCache healthStatusCache,
            PostgresHealthIndicator postgresHealthIndicator,
            HealthCheckProperties properties,
            TaskScheduler taskScheduler) {
        long interval = properties.getServiceInterval("postgres");
        return new ServiceHealthCheckScheduler("postgres", postgresHealthIndicator, healthStatusCache, interval, taskScheduler);
    }

    @Bean
    public ServiceHealthCheckScheduler redisHealthCheckScheduler(
            HealthStatusCache healthStatusCache,
            RedisHealthIndicator redisHealthIndicator,
            HealthCheckProperties properties,
            TaskScheduler taskScheduler) {
        long interval = properties.getServiceInterval("redis");
        return new ServiceHealthCheckScheduler("redis", redisHealthIndicator, healthStatusCache, interval, taskScheduler);
    }

    @Bean
    public ServiceHealthCheckScheduler rabbitmqHealthCheckScheduler(
            HealthStatusCache healthStatusCache,
            RabbitMqHealthIndicator rabbitMqHealthIndicator,
            HealthCheckProperties properties,
            TaskScheduler taskScheduler) {
        long interval = properties.getServiceInterval("rabbitmq");
        return new ServiceHealthCheckScheduler("rabbitmq", rabbitMqHealthIndicator, healthStatusCache, interval, taskScheduler);
    }

    @Bean
    public ServiceHealthCheckScheduler mongodbHealthCheckScheduler(
            HealthStatusCache healthStatusCache,
            MongoDbHealthIndicator mongoDbHealthIndicator,
            HealthCheckProperties properties,
            TaskScheduler taskScheduler) {
        long interval = properties.getServiceInterval("mongodb");
        return new ServiceHealthCheckScheduler("mongodb", mongoDbHealthIndicator, healthStatusCache, interval, taskScheduler);
    }

    @Bean
    public ServiceHealthCheckScheduler mockWebServerHealthCheckScheduler(
            HealthStatusCache healthStatusCache,
            MockWebServerHealthIndicator mockWebServerHealthIndicator,
            HealthCheckProperties properties,
            TaskScheduler taskScheduler) {
        long interval = properties.getServiceInterval("mockWebServer");
        return new ServiceHealthCheckScheduler("mockWebServer", mockWebServerHealthIndicator, healthStatusCache, interval, taskScheduler);
    }
}

