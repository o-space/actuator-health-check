package com.chainsea.healthcheck.service.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis step for Saga pattern.
 * Execute: Cache task status in Redis (local transaction)
 * Compensate: Delete the cached status
 */
@Component
public class RedisSagaStep implements SagaStep {

    private static final Logger logger = LoggerFactory.getLogger(RedisSagaStep.class);
    private static final String KEY_PREFIX = "task:status:";

    private final StringRedisTemplate redisTemplate;

    public RedisSagaStep(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getStepName() {
        return "Redis";
    }

    @Override
    public boolean execute(SagaContext sagaContext, String taskId, List<String> serviceNames) {
        try {
            logger.info("Redis Saga: Executing step for task {}", taskId);
            String statusKey = KEY_PREFIX + taskId;

            // Execute local transaction - cache immediately
            redisTemplate.opsForValue().set(statusKey, "PROCESSING", 1, TimeUnit.HOURS);

            // Store key in context for compensation
            sagaContext.addStepData("redis", statusKey);
            sagaContext.addStepResult("redis", "PROCESSING");

            logger.info("Redis Saga: Task {} status cached", taskId);
            return true;
        } catch (Exception e) {
            logger.error("Redis Saga: Failed to execute step for task {}", taskId, e);
            return false;
        }
    }

    @Override
    public void compensate(SagaContext sagaContext) {
        try {
            logger.info("Redis Saga: Compensating step");
            String statusKey = (String) sagaContext.getStepData("redis");
            if (statusKey != null) {
                redisTemplate.delete(statusKey);
                logger.info("Redis Saga: Cached status deleted for key {}", statusKey);
            }
        } catch (Exception e) {
            logger.error("Redis Saga: Failed to compensate", e);
        }
    }
}
