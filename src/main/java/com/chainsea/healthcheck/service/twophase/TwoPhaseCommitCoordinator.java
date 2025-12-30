package com.chainsea.healthcheck.service.twophase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Coordinator for Two-Phase Commit (2PC) protocol.
 * <p>
 * Scenario: Batch Health Check Task
 * - Step 1: Save task to PostgreSQL
 * - Step 2: Cache task status in Redis
 * - Step 3: Log task details to MongoDB
 * - Step 4: Send notification via RabbitMQ
 * <p>
 * All steps must succeed or all must be rolled back.
 */
@Component
public class TwoPhaseCommitCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(TwoPhaseCommitCoordinator.class);

    private final List<TwoPhaseCommitParticipant> participants;

    public TwoPhaseCommitCoordinator(
            PostgresParticipant postgresParticipant,
            RedisParticipant redisParticipant,
            MongoDbParticipant mongodbParticipant,
            RabbitMqParticipant rabbitmqParticipant) {
        this.participants = List.of(
                postgresParticipant,
                redisParticipant,
                mongodbParticipant,
                rabbitmqParticipant
        );
    }

    /**
     * Execute a distributed transaction using 2PC protocol.
     *
     * @param taskId       the task ID
     * @param serviceNames list of service names to check
     * @return true if transaction committed successfully, false otherwise
     */
    public boolean executeTransaction(String taskId, List<String> serviceNames) {
        String transactionId = UUID.randomUUID().toString();
        logger.info("Starting 2PC transaction: {}", transactionId);

        try {
            // transaction.start()
            // Phase 1: Prepare (Voting Phase)
            if (!preparePhase(transactionId, taskId, serviceNames)) {
                logger.warn("Prepare phase failed for transaction: {}", transactionId);
                rollbackPhase(transactionId);
                return false;
            }

            // Phase 2: Commit
            if (!commitPhase(transactionId)) {
                logger.error("Commit phase failed for transaction: {}", transactionId);
                rollbackPhase(transactionId);
                return false;
            }
            // transaction.commit()

            logger.info("Transaction {} committed successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("Transaction {} failed with exception", transactionId, e);
            rollbackPhase(transactionId);
            return false;
        }
    }

    /**
     * Phase 1: Prepare - All participants vote on whether they can commit.
     */
    private boolean preparePhase(String transactionId, String taskId, List<String> serviceNames) {
        logger.info("Phase 1: Prepare phase started for transaction: {}", transactionId);

        boolean allPrepared = participants.stream().allMatch(p -> {
            boolean prepared = p.prepare(transactionId, taskId, serviceNames);
            if (!prepared) {
                logger.error("{} failed to prepare", p.getClass().getSimpleName());
            }
            return prepared;
        });

        if (allPrepared) {
            logger.info("Phase 1: All participants prepared successfully");
            return true;
        } else {
            logger.warn("Phase 1: Some participants failed to prepare");
            return false;
        }
    }

    /**
     * Phase 2: Commit - All participants commit their changes.
     */
    private boolean commitPhase(String transactionId) {
        logger.info("Phase 2: Commit phase started for transaction: {}", transactionId);

        boolean allCommitted = participants.stream().allMatch(p -> {
            boolean committed = p.commit(transactionId);
            if (!committed) {
                logger.error("{} failed to commit", p.getClass().getSimpleName());
            }
            return committed;
        });

        if (allCommitted) {
            logger.info("Phase 2: All participants committed successfully");
            return true;
        } else {
            logger.error("Phase 2: Some participants failed to commit");
            return false;
        }
    }

    /**
     * Rollback - All participants rollback their changes.
     */
    private void rollbackPhase(String transactionId) {
        logger.info("Rollback phase started for transaction: {}", transactionId);
        // Rollback in reverse order
        participants.reversed().forEach(p -> p.rollback(transactionId));
        logger.info("Rollback phase completed for transaction: {}", transactionId);
    }
}
