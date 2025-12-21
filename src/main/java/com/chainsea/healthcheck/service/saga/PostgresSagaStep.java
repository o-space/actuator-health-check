package com.chainsea.healthcheck.service.saga;

import com.chainsea.healthcheck.model.BatchHealthCheckTask;
import com.chainsea.healthcheck.model.TaskStatus;
import com.chainsea.healthcheck.repository.BatchHealthCheckTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PostgreSQL step for Saga pattern.
 * Execute: Save task to database (local transaction)
 * Compensate: Delete or mark task as failed
 */
@Component
public class PostgresSagaStep implements SagaStep {

    private static final Logger logger = LoggerFactory.getLogger(PostgresSagaStep.class);
    private static final String STEP_NAME = "PostgreSQL";

    private final BatchHealthCheckTaskRepository repository;

    public PostgresSagaStep(BatchHealthCheckTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public String getStepName() {
        return STEP_NAME;
    }

    @Override
    public boolean execute(SagaContext sagaContext, String taskId, List<String> serviceNames) {
        try {
            logger.info("PostgreSQL Saga: Executing step for task {}", taskId);
            // Execute local transaction - save immediately
            BatchHealthCheckTask task = new BatchHealthCheckTask(taskId, serviceNames);
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            BatchHealthCheckTask saved = repository.save(task);

            // Store task ID in context for compensation
            sagaContext.addStepData(STEP_NAME, saved.getId());
            sagaContext.addStepResult(STEP_NAME, saved);

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
            Long taskId = (Long) sagaContext.getStepData(STEP_NAME);
            if (taskId != null) {
                BatchHealthCheckTask task = repository.findById(taskId).orElse(null);
                if (task != null) {
                    task.setStatus(TaskStatus.FAILED);
                    repository.save(task);
                    logger.info("PostgreSQL Saga: Task {} marked as FAILED", taskId);
                }
            }
        } catch (Exception e) {
            logger.error("PostgreSQL Saga: Failed to compensate", e);
        }
    }

}
