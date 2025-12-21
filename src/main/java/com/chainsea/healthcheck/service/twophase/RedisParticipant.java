package com.chainsea.healthcheck.service.twophase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis participant for 2PC protocol.
 * Manages task status cache in Redis.
 */
@Component
public class RedisParticipant implements TwoPhaseCommitParticipant {

    private static final Logger logger = LoggerFactory.getLogger(RedisParticipant.class);
    private static final String KEY_PREFIX = "task:status:";
    private static final String PREPARE_PREFIX = "task:prepare:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, String> preparedKeys = new ConcurrentHashMap<>();

    public RedisParticipant(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean prepare(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("Redis: Preparing transaction {}", transactionId);
            String prepareKey = PREPARE_PREFIX + transactionId;
            String statusKey = KEY_PREFIX + taskId;

            // Store prepared status in temporary key
            redisTemplate.opsForValue().set(prepareKey, "PROCESSING", 10, TimeUnit.MINUTES);
            preparedKeys.put(transactionId, statusKey);

            logger.info("Redis: Prepared transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("Redis: Failed to prepare transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean commit(String transactionId) {
        try {
            logger.info("Redis: Committing transaction {}", transactionId);
            String prepareKey = PREPARE_PREFIX + transactionId;
            String statusKey = preparedKeys.get(transactionId);

            if (statusKey == null) {
                logger.error("Redis: No prepared key found for transaction {}", transactionId);
                return false;
            }

            // Move from prepare key to actual status key
            String status = redisTemplate.opsForValue().get(prepareKey);
            if (status != null) {
                redisTemplate.opsForValue().set(statusKey, "COMPLETED", 1, TimeUnit.HOURS);
                redisTemplate.delete(prepareKey);
            }
            preparedKeys.remove(transactionId);

            logger.info("Redis: Committed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("Redis: Failed to commit transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void rollback(String transactionId) {
        try {
            logger.info("Redis: Rolling back transaction {}", transactionId);
            String prepareKey = PREPARE_PREFIX + transactionId;
            redisTemplate.delete(prepareKey);
            preparedKeys.remove(transactionId);
            logger.info("Redis: Rolled back transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("Redis: Failed to rollback transaction {}", transactionId, e);
        }
    }
}
