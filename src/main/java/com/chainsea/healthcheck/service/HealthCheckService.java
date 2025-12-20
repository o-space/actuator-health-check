package com.chainsea.healthcheck.service;

import com.chainsea.healthcheck.model.HealthCheckRecord;

import java.net.URL;
import java.util.List;
import java.util.Optional;

public interface HealthCheckService {

    /**
     * Performs a health check for the specified service at the given URL.
     *
     * @param serviceName    the name of the service to check
     * @param healthCheckUrl the URL to perform the health check against
     * @return the health check record with the result
     */
    HealthCheckRecord check(String serviceName, URL healthCheckUrl);

    /**
     * Retrieves the health check history for a specific service.
     *
     * @param serviceName the name of the service
     * @return list of health check records ordered by checked time descending
     */
    List<HealthCheckRecord> getHealthChecks(String serviceName);

    /**
     * Retrieves the latest health check record for a specific service.
     *
     * @param serviceName the name of the service
     * @return optional health check record, empty if no records found
     */
    Optional<HealthCheckRecord> getLatestHealthCheck(String serviceName);

    /**
     * Retrieves recent health check records within the specified time window.
     *
     * @param hours the number of hours to look back
     * @return list of health check records within the time window
     */
    List<HealthCheckRecord> getHealthChecks(int hours);

    /**
     * Retrieves health check history for a specific service within the specified time window.
     *
     * @param serviceName the name of the service
     * @param hours       the number of hours to look back
     * @return list of health check records for the service within the time window, ordered by checked time descending
     */
    List<HealthCheckRecord> getHealthChecks(String serviceName, int hours);

    /**
     * Gets the count of failed health checks for a specific service.
     *
     * @param serviceName the name of the service
     * @return the count of failed health checks
     */
    long getFailureCount(String serviceName);

    /**
     * Retrieves a health check record by its ID.
     *
     * @param id the ID of the health check record
     * @return optional health check record, empty if not found
     */
    Optional<HealthCheckRecord> getHealthCheckById(Long id);
}
