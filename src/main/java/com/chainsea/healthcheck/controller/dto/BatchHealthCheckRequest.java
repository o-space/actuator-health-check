package com.chainsea.healthcheck.controller.dto;

import java.util.List;

public record BatchHealthCheckRequest(
        String taskId,
        List<String> serviceNames
) {
}
