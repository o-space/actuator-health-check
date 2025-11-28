package com.chainsea.healthcheck.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HealthStatusCache {

    private final Map<String, CachedHealth> cache = new ConcurrentHashMap<>();

    public void updateHealth(String serviceName, Health health) {
        cache.put(serviceName, new CachedHealth(health, Instant.now()));
    }

    public Map<String, CachedHealth> getAllCachedHealths() {
        return new ConcurrentHashMap<>(cache);
    }

    public record CachedHealth(Health health, Instant lastUpdateTime) {
    }
}

