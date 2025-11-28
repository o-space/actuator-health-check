package com.chainsea.healthcheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "health-check")
public class HealthCheckProperties {

    private Set<String> criticalServices = new HashSet<>();
    private Set<String> nonCriticalServices = new HashSet<>();

    public Set<String> getCriticalServices() {
        return criticalServices;
    }

    public void setCriticalServices(Set<String> criticalServices) {
        this.criticalServices = criticalServices;
    }

    public Set<String> getNonCriticalServices() {
        return nonCriticalServices;
    }

    public void setNonCriticalServices(Set<String> nonCriticalServices) {
        this.nonCriticalServices = nonCriticalServices;
    }
}

