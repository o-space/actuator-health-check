package com.chainsea.healthcheck.controller.dto;

public record ServiceStatsResponse(
        String serviceName,
        long failureCount,
        String latestStatus,
        boolean hasRecords
) {
}
