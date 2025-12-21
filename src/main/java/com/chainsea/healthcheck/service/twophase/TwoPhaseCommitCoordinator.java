package com.chainsea.healthcheck.service.twophase;

import com.chainsea.healthcheck.model.PC2TransactionContext;
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

    private final TwoPhaseCommitParticipant postgresParticipant;
    private final TwoPhaseCommitParticipant redisParticipant;
    private final TwoPhaseCommitParticipant mongodbParticipant;
    private final TwoPhaseCommitParticipant rabbitmqParticipant;

    public TwoPhaseCommitCoordinator(
            PostgresParticipant postgresParticipant,
            RedisParticipant redisParticipant,
            MongoDbParticipant mongodbParticipant,
            RabbitMqParticipant rabbitmqParticipant) {
        this.postgresParticipant = postgresParticipant;
        this.redisParticipant = redisParticipant;
        this.mongodbParticipant = mongodbParticipant;
        this.rabbitmqParticipant = rabbitmqParticipant;
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
        PC2TransactionContext context = new PC2TransactionContext(transactionId);
        context.addParticipant("postgres", postgresParticipant);
        context.addParticipant("redis", redisParticipant);
        context.addParticipant("mongodb", mongodbParticipant);
        context.addParticipant("rabbitmq", rabbitmqParticipant);

        logger.info("Starting 2PC transaction: {}", transactionId);

        try {
            // Phase 1: Prepare (Voting Phase)
            if (!preparePhase(context, taskId, serviceNames)) {
                logger.warn("Prepare phase failed for transaction: {}", transactionId);
                rollbackPhase(context);
                return false;
            }

            // Phase 2: Commit
            if (!commitPhase(context)) {
                logger.error("Commit phase failed for transaction: {}", transactionId);
                rollbackPhase(context);
                return false;
            }

            logger.info("Transaction {} committed successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("Transaction {} failed with exception", transactionId, e);
            rollbackPhase(context);
            return false;
        }
    }

    /**
     * Phase 1: Prepare - All participants vote on whether they can commit.
     */
    private boolean preparePhase(PC2TransactionContext context, String taskId, List<String> serviceNames) {
        context.setCurrentPhase(PC2TransactionContext.TransactionPhase.PREPARING);
        logger.info("Phase 1: Prepare phase started for transaction: {}", context.getTransactionId());

        boolean allPrepared = true;

        // Prepare PostgreSQL
        if (!postgresParticipant.prepare(context.getTransactionId(), taskId, serviceNames)) {
            logger.error("PostgreSQL participant failed to prepare");
            allPrepared = false;
        }

        // Prepare Redis
        if (!redisParticipant.prepare(context.getTransactionId(), taskId, serviceNames)) {
            logger.error("Redis participant failed to prepare");
            allPrepared = false;
        }

        // Prepare MongoDB
        if (!mongodbParticipant.prepare(context.getTransactionId(), taskId, serviceNames)) {
            logger.error("MongoDB participant failed to prepare");
            allPrepared = false;
        }

        // Prepare RabbitMQ
        if (!rabbitmqParticipant.prepare(context.getTransactionId(), taskId, serviceNames)) {
            logger.error("RabbitMQ participant failed to prepare");
            allPrepared = false;
        }

        if (allPrepared) {
            context.setCurrentPhase(PC2TransactionContext.TransactionPhase.PREPARED);
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
    private boolean commitPhase(PC2TransactionContext context) {
        context.setCurrentPhase(PC2TransactionContext.TransactionPhase.COMMITTING);
        logger.info("Phase 2: Commit phase started for transaction: {}", context.getTransactionId());

        boolean allCommitted = true;

        // Commit PostgreSQL
        if (!postgresParticipant.commit(context.getTransactionId())) {
            logger.error("PostgreSQL participant failed to commit");
            allCommitted = false;
        }

        // Commit Redis
        if (!redisParticipant.commit(context.getTransactionId())) {
            logger.error("Redis participant failed to commit");
            allCommitted = false;
        }

        // Commit MongoDB
        if (!mongodbParticipant.commit(context.getTransactionId())) {
            logger.error("MongoDB participant failed to commit");
            allCommitted = false;
        }

        // Commit RabbitMQ
        if (!rabbitmqParticipant.commit(context.getTransactionId())) {
            logger.error("RabbitMQ participant failed to commit");
            allCommitted = false;
        }

        if (allCommitted) {
            context.setCurrentPhase(PC2TransactionContext.TransactionPhase.COMMITTED);
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
    private void rollbackPhase(PC2TransactionContext context) {
        context.setCurrentPhase(PC2TransactionContext.TransactionPhase.ROLLING_BACK);
        logger.info("Rollback phase started for transaction: {}", context.getTransactionId());

        // Rollback in reverse order
        rabbitmqParticipant.rollback(context.getTransactionId());
        mongodbParticipant.rollback(context.getTransactionId());
        redisParticipant.rollback(context.getTransactionId());
        postgresParticipant.rollback(context.getTransactionId());

        context.setCurrentPhase(PC2TransactionContext.TransactionPhase.ROLLED_BACK);
        logger.info("Rollback phase completed for transaction: {}", context.getTransactionId());
    }
}
