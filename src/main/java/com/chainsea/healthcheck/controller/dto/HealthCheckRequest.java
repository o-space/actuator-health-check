package com.chainsea.healthcheck.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URL;

public record HealthCheckRequest(
        @NotBlank(message = "Service name is required")
        String serviceName,

        @NotNull(message = "URL is required")
        URL url
) {
}
