package com.chainsea.healthcheck.service.tcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis participant for TCC pattern.
 * Try: Set status to "RESERVED" with lock
 * Confirm: Change status to "COMPLETED"
 * Cancel: Delete the reserved key
 */
@Component
public class RedisTccParticipant implements TccParticipant {

    private static final Logger logger = LoggerFactory.getLogger(RedisTccParticipant.class);
    private static final String KEY_PREFIX = "task:status:";
    private static final String LOCK_PREFIX = "task:lock:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, String> reservedKeys = new ConcurrentHashMap<>();

    public RedisTccParticipant(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryExecute(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("Redis TCC: Trying transaction {}", transactionId);
            String statusKey = KEY_PREFIX + taskId;
            String lockKey = LOCK_PREFIX + taskId;

            // Try to acquire lock and set reserved status
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, transactionId, 10, TimeUnit.MINUTES);
            if (Boolean.TRUE.equals(lockAcquired)) {
                redisTemplate.opsForValue().set(statusKey, "RESERVED", 10, TimeUnit.MINUTES);
                reservedKeys.put(transactionId, statusKey);
                logger.info("Redis TCC: Tried transaction {} successfully", transactionId);
                return true;
            } else {
                logger.warn("Redis TCC: Failed to acquire lock for transaction {}", transactionId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Redis TCC: Failed to try transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean confirm(String transactionId) {
        try {
            logger.info("Redis TCC: Confirming transaction {}", transactionId);
            String statusKey = reservedKeys.get(transactionId);
            if (statusKey == null) {
                logger.error("Redis TCC: No reserved key found for transaction {}", transactionId);
                return false;
            }

            // Change status from RESERVED to COMPLETED
            redisTemplate.opsForValue().set(statusKey, "COMPLETED", 1, TimeUnit.HOURS);
            String taskId = statusKey.replace(KEY_PREFIX, "");
            String lockKey = LOCK_PREFIX + taskId;
            redisTemplate.delete(lockKey);
            reservedKeys.remove(transactionId);

            logger.info("Redis TCC: Confirmed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("Redis TCC: Failed to confirm transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void cancel(String transactionId) {
        try {
            logger.info("Redis TCC: Cancelling transaction {}", transactionId);
            String statusKey = reservedKeys.remove(transactionId);
            if (statusKey != null) {
                String taskId = statusKey.replace(KEY_PREFIX, "");
                String lockKey = LOCK_PREFIX + taskId;
                redisTemplate.delete(statusKey);
                redisTemplate.delete(lockKey);
            }
            logger.info("Redis TCC: Cancelled transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("Redis TCC: Failed to cancel transaction {}", transactionId, e);
        }
    }
}
