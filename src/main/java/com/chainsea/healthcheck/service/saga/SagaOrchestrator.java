package com.chainsea.healthcheck.service.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrator for Saga pattern (Orchestration style).
 * <p>
 * Scenario: Batch Health Check Task
 * - Step 1: Save task to PostgreSQL (local transaction)
 * - Step 2: Cache task status in Redis (local transaction)
 * - Step 3: Log task details to MongoDB (local transaction)
 * - Step 4: Send notification via RabbitMQ (local transaction)
 * <p>
 * If any step fails, compensate all previous steps in reverse order.
 * <p>
 * Saga Pattern Characteristics:
 * - Each step executes a local transaction immediately
 * - No two-phase commit overhead
 * - Better performance than 2PC
 * - Requires compensation logic for each step
 */
@Component
public class SagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final List<SagaStep> sagaSteps;

    public SagaOrchestrator(
            PostgresSagaStep postgresStep,
            RedisSagaStep redisStep,
            MongoDbSagaStep mongodbStep,
            RabbitMqSagaStep rabbitmqStep) {
        this.sagaSteps = List.of(postgresStep, redisStep, mongodbStep, rabbitmqStep);
    }

    /**
     * Execute a distributed transaction using Saga pattern.
     *
     * @param taskId       the task ID
     * @param serviceNames list of service names to check
     * @return true if all steps executed successfully, false otherwise
     */
    public boolean executeSaga(String taskId, List<String> serviceNames) {
        String sagaId = UUID.randomUUID().toString();
        SagaContext context = new SagaContext(sagaId);
        logger.info("Starting Saga transaction: {}", sagaId);

        try {
            // Execute each step sequentially
            for (int i = 0; i < sagaSteps.size(); i++) {
                SagaStep step = sagaSteps.get(i);
                context.setCurrentStepIndex(i);
                logger.info("Saga {}: Executing step {} - {}", sagaId, i + 1, step.getStepName());

                if (!step.execute(context, taskId, serviceNames)) {
                    logger.error("Saga {}: Step {} ({}) failed, starting compensation", sagaId, i + 1, step.getStepName());
                    compensate(context, i - 1);
                    return false;
                }

                logger.info("Saga {}: Step {} ({}) completed successfully", sagaId, i + 1, step.getStepName());
            }

            logger.info("Saga {}: All steps executed successfully", sagaId);
            return true;
        } catch (Exception e) {
            logger.error("Saga {}: Exception occurred during execution", sagaId, e);
            compensate(context, context.getCurrentStepIndex());
            return false;
        }
    }

    /**
     * Compensate all executed steps in reverse order.
     *
     * @param context               the saga context
     * @param lastExecutedStepIndex the index of the last successfully executed step
     */
    private void compensate(SagaContext context, int lastExecutedStepIndex) {
        logger.info("Saga {}: Starting compensation from step {}", context.getSagaId(), lastExecutedStepIndex + 1);

        // Compensate in reverse order
        for (int i = lastExecutedStepIndex; i >= 0; i--) {
            SagaStep step = sagaSteps.get(i);
            try {
                logger.info("Saga {}: Compensating step {} - {}", context.getSagaId(), i + 1, step.getStepName());
                step.compensate(context);
                logger.info("Saga {}: Step {} compensated successfully", context.getSagaId(), i + 1);
            } catch (Exception e) {
                logger.error("Saga {}: Failed to compensate step {} ({})", context.getSagaId(), i + 1, step.getStepName(), e);
                // Continue compensating other steps even if one fails
            }
        }

        logger.info("Saga {}: Compensation completed", context.getSagaId());
    }

}
