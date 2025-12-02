package com.chainsea.healthcheck.config;

public record ServiceConfig(String name, Long interval) {
    public ServiceConfig {
        if (interval == null) {
            interval = 5000L;
        }
    }
}

