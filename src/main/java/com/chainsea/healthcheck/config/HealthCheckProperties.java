package com.chainsea.healthcheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "health-check")
public record HealthCheckProperties(
        List<ServiceConfig> criticalServices,
        List<ServiceConfig> nonCriticalServices,
        SchedulerConfig scheduler
) {
    public HealthCheckProperties {
        if (criticalServices == null) {
            criticalServices = new ArrayList<>();
        } else {
            criticalServices = new ArrayList<>(criticalServices);
        }
        if (nonCriticalServices == null) {
            nonCriticalServices = new ArrayList<>();
        } else {
            nonCriticalServices = new ArrayList<>(nonCriticalServices);
        }
    }

    public Set<String> getCriticalServiceNames() {
        return criticalServices.stream()
                .map(ServiceConfig::name)
                .collect(Collectors.toSet());
    }

    public Set<String> getNonCriticalServiceNames() {
        return nonCriticalServices.stream()
                .map(ServiceConfig::name)
                .collect(Collectors.toSet());
    }

    public Map<String, Long> getServiceIntervals() {
        Map<String, Long> intervals = criticalServices.stream()
                .collect(Collectors.toMap(ServiceConfig::name, ServiceConfig::interval));
        nonCriticalServices.forEach(service ->
                intervals.put(service.name(), service.interval()));
        return intervals;
    }

    public Long getServiceInterval(String serviceName) {
        return criticalServices.stream()
                .filter(s -> s.name().equals(serviceName))
                .findFirst()
                .map(ServiceConfig::interval)
                .orElseGet(() -> nonCriticalServices.stream()
                        .filter(s -> s.name().equals(serviceName))
                        .findFirst()
                        .map(ServiceConfig::interval)
                        .orElse(5000L));
    }
}

