package com.chainsea.healthcheck.service.tcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Coordinator for TCC (Try-Confirm-Cancel) pattern.
 * <p>
 * Scenario: Batch Health Check Task
 * - Step 1: Try to save task to PostgreSQL (reserve record)
 * - Step 2: Try to cache task status in Redis (reserve cache slot)
 * - Step 3: Try to log task details to MongoDB (reserve log entry)
 * - Step 4: Try to send notification via RabbitMQ (reserve message)
 * <p>
 * If all Try operations succeed, Confirm all. Otherwise, Cancel all.
 */
@Component
public class TccCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TccCoordinator.class);

    private final List<TccParticipant> participants;

    public TccCoordinator(
            PostgresTccParticipant postgresParticipant,
            RedisTccParticipant redisParticipant,
            MongoDbTccParticipant mongodbParticipant,
            RabbitMqTccParticipant rabbitmqParticipant) {
        participants = List.of(
                postgresParticipant,
                redisParticipant,
                mongodbParticipant,
                rabbitmqParticipant
        );
    }

    /**
     * Execute a distributed transaction using TCC pattern.
     *
     * @param taskId       the task ID
     * @param serviceNames list of service names to check
     * @return true if transaction confirmed successfully, false otherwise
     */
    public boolean executeTransaction(String taskId, List<String> serviceNames) {
        String transactionId = UUID.randomUUID().toString();
        logger.info("Starting TCC transaction: {}", transactionId);

        try {
            // Phase 1: Try - Reserve resources
            if (!tryPhase(transactionId, taskId, serviceNames)) {
                logger.warn("Try phase failed for transaction: {}", transactionId);
                cancelPhase(transactionId);
                return false;
            }

            // Phase 2: Confirm - Commit reserved resources
            if (!confirmPhase(transactionId)) {
                logger.error("Confirm phase failed for transaction: {}", transactionId);
                cancelPhase(transactionId);
                return false;
            }

            logger.info("Transaction {} confirmed successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("Transaction {} failed with exception", transactionId, e);
            cancelPhase(transactionId);
            return false;
        }
    }

    /**
     * Phase 1: Try - All participants try to reserve resources.
     */
    private boolean tryPhase(String transactionId, String taskId, List<String> serviceNames) {
        logger.info("Phase 1: Try phase started for transaction: {}", transactionId);

        boolean allTried = participants.stream().allMatch(p -> {
            boolean tried = p.tryExecute(transactionId, taskId, serviceNames);
            if (!tried) {
                logger.error("{} failed to try", p.getClass().getSimpleName());
            }
            return tried;
        });

        if (allTried) {
            logger.info("Phase 1: All participants tried successfully");
            return true;
        } else {
            logger.warn("Phase 1: Some participants failed to try");
            return false;
        }
    }

    /**
     * Phase 2: Confirm - All participants confirm their reserved resources.
     */
    private boolean confirmPhase(String transactionId) {
        logger.info("Phase 2: Confirm phase started for transaction: {}", transactionId);

        boolean allConfirmed = participants.stream().allMatch(p -> {
            boolean confirmed = p.confirm(transactionId);
            if (!confirmed) {
                logger.error("{} failed to confirm", p.getClass().getSimpleName());
            }
            return confirmed;
        });

        if (allConfirmed) {
            logger.info("Phase 2: All participants confirmed successfully");
            return true;
        } else {
            logger.error("Phase 2: Some participants failed to confirm");
            return false;
        }
    }

    /**
     * Cancel - All participants cancel their reserved resources.
     */
    private void cancelPhase(String transactionId) {
        logger.info("Cancel phase started for transaction: {}", transactionId);

        // Cancel in reverse order
        participants.reversed().forEach(p -> p.cancel(transactionId));

        logger.info("Cancel phase completed for transaction: {}", transactionId);
    }
}
