package com.chainsea.healthcheck.service.saga;

import com.chainsea.healthcheck.model.BatchHealthCheckTask;
import com.chainsea.healthcheck.repository.BatchHealthCheckTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PostgreSQL step for Saga pattern.
 * Execute: Save task to database (local transaction)
 * Compensate: Delete or mark task as failed
 */
@Component
public class PostgresSagaStep implements SagaStep {

    private static final Logger logger = LoggerFactory.getLogger(PostgresSagaStep.class);

    private final BatchHealthCheckTaskRepository repository;

    public PostgresSagaStep(BatchHealthCheckTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getStepName() {
        return "PostgreSQL";
    }

    @Override
    public boolean execute(SagaContext sagaContext, String taskId, List<String> serviceNames) {
        try {
            logger.info("PostgreSQL Saga: Executing step for task {}", taskId);
            // Execute local transaction - save immediately
            BatchHealthCheckTask task = new BatchHealthCheckTask(taskId, serviceNames);
            task.setStatus("PROCESSING");
            BatchHealthCheckTask saved = repository.save(task);

            // Store task ID in context for compensation
            sagaContext.addStepData("postgres", saved.getId());
            sagaContext.addStepResult("postgres", saved);

            logger.info("PostgreSQL Saga: Task {} saved with ID {}", taskId, saved.getId());
            return true;
        } catch (Exception e) {
            logger.error("PostgreSQL Saga: Failed to execute step for task {}", taskId, e);
            return false;
        }
    }

    @Override
    public void compensate(SagaContext sagaContext) {
        try {
            logger.info("PostgreSQL Saga: Compensating step");
            Long taskId = (Long) sagaContext.getStepData("postgres");
            if (taskId != null) {
                BatchHealthCheckTask task = repository.findById(taskId).orElse(null);
                if (task != null) {
                    task.setStatus("FAILED");
                    repository.save(task);
                    logger.info("PostgreSQL Saga: Task {} marked as FAILED", taskId);
                }
            }
        } catch (Exception e) {
            logger.error("PostgreSQL Saga: Failed to compensate", e);
        }
    }

    /**
     * Mark task as completed after all saga steps succeed.
     *
     * @param sagaContext the saga context
     * @param taskId      the task ID
     */
    public void markTaskAsCompleted(SagaContext sagaContext, String taskId) {
        try {
            logger.info("PostgreSQL Saga: Marking task {} as COMPLETED", taskId);
            BatchHealthCheckTask task = null;

            // Try to find by ID first (from context)
            Long id = (Long) sagaContext.getStepData("postgres");
            if (id != null) {
                task = repository.findById(id).orElse(null);
            }

            // If not found by ID, try to find by taskId
            if (task == null) {
                logger.warn("PostgreSQL Saga: Task not found by ID {}, trying to find by taskId {}", id, taskId);
                task = repository.findByTaskId(taskId).orElse(null);
            }

            if (task != null) {
                task.setStatus("COMPLETED");
                task.setCompletedAt(java.time.LocalDateTime.now());
                repository.save(task);
                logger.info("PostgreSQL Saga: Task {} marked as COMPLETED with completedAt timestamp", taskId);
            } else {
                logger.error("PostgreSQL Saga: Task {} not found in database", taskId);
            }
        } catch (Exception e) {
            logger.error("PostgreSQL Saga: Failed to mark task {} as COMPLETED", taskId, e);
        }
    }
}
