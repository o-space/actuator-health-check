package com.chainsea.healthcheck.service;

import com.chainsea.healthcheck.model.HealthCheckRecord;
import com.chainsea.healthcheck.repository.HealthCheckRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    private final HealthCheckRecordRepository repository;
    private final RestClient restClient;

    public HealthCheckServiceImpl(HealthCheckRecordRepository repository, RestClient restClient) {
        this.repository = repository;
        this.restClient = restClient;
    }

    private static Map<String, Object> getErrorDetails(Exception exception) {
        String errorMessage = exception.getMessage();
        // Truncate error message if too long (JSON can handle large strings, but keep reasonable)
        if (errorMessage != null && errorMessage.length() > 5000) {
            errorMessage = errorMessage.substring(0, 4997) + "...";
        }
        return Map.of(
                "message", "Health check failed",
                "error", errorMessage != null ? errorMessage : exception.getClass().getSimpleName(),
                "errorType", exception.getClass().getName());
    }

    @Override
    @Transactional
    public HealthCheckRecord check(String serviceName, URL healthCheckUrl) {
        logger.info("Performing health check for service: {} at URL: {}", serviceName, healthCheckUrl);
        long startTime = System.currentTimeMillis();

        try {
            var response = restClient.get().uri(healthCheckUrl.toURI()).retrieve().toEntity(String.class);
            long responseTime = System.currentTimeMillis() - startTime;

            boolean isOk = response.getStatusCode().equals(HttpStatus.OK);
            String status = isOk ? "UP" : "DEGRADED";
            Map<String, Object> details = Map.of(
                    "message", isOk ? "Health check successful" : "Health check returned non-OK status",
                    "responseBody", response.getBody(),
                    "statusCode", response.getStatusCode().value()
            );

            HealthCheckRecord saved = repository.save(new HealthCheckRecord(serviceName, status, details, responseTime));
            logger.info("Health check completed for service: {} with status: {} in {}ms", serviceName, status, responseTime);
            return saved;
        } catch (Exception ex) {
            long responseTime = System.currentTimeMillis() - startTime;
            logger.error("Health check failed for service: {}", serviceName, ex);
            return repository.save(new HealthCheckRecord(serviceName, "DOWN", getErrorDetails(ex), responseTime));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthCheckRecord> getHealthChecks(String serviceName) {
        return repository.findByServiceNameOrderByCheckedAtDesc(serviceName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthCheckRecord> getLatestHealthCheck(String serviceName) {
        return repository.findFirstByServiceNameOrderByCheckedAtDesc(serviceName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthCheckRecord> getHealthChecks(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return repository.findRecentRecords(since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthCheckRecord> getHealthChecks(String serviceName, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return repository.findByServiceNameAndCheckedAtAfterOrderByCheckedAtDesc(serviceName, since);
    }

    @Override
    @Transactional(readOnly = true)
    public long getFailureCount(String serviceName) {
        return repository.countByServiceNameAndStatus(serviceName, "DOWN");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HealthCheckRecord> getHealthCheckById(Long id) {
        return repository.findById(id);
    }
}
