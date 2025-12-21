package com.chainsea.healthcheck.service.twophase;

import com.chainsea.healthcheck.model.BatchHealthCheckTask;
import com.chainsea.healthcheck.model.TaskStatus;
import com.chainsea.healthcheck.repository.BatchHealthCheckTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL participant for 2PC protocol.
 * <p>
 * Uses database transactions and locks to ensure data consistency:
 * - Prepare phase: Saves task to database with PREPARED status (persisted but not committed)
 * - Commit phase: Updates status to COMPLETED
 * - Rollback phase: Deletes the prepared record
 * <p>
 * This ensures that if the coordinator crashes, prepared data is still in the database
 * and can be recovered or cleaned up.
 */
@Component
public class PostgresParticipant implements TwoPhaseCommitParticipant {

    private static final Logger logger = LoggerFactory.getLogger(PostgresParticipant.class);

    private final BatchHealthCheckTaskRepository repository;
    // Store transactionId -> taskId mapping for commit/rollback
    private final Map<String, String> transactionToTaskId = new ConcurrentHashMap<>();

    public PostgresParticipant(BatchHealthCheckTaskRepository repository) {
        this.repository = repository;
    }

    /**
     * Phase 1: Prepare - Save task to database with PREPARED status.
     * <p>
     * This uses database transaction to ensure atomicity.
     * The task_id has UNIQUE constraint to prevent concurrent conflicts.
     * If another transaction tries to prepare the same taskId, it will fail with DataIntegrityViolationException.
     */
    @Override
    @Transactional
    public boolean prepare(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("PostgreSQL: Preparing transaction {} for task {}", transactionId, taskId);

            // Check if task already exists (could be from a previous failed transaction)
            repository.findByTaskId(taskId).ifPresent(existingTask -> {
                if (existingTask.getStatus() == TaskStatus.PREPARED) {
                    // Clean up stale prepared task
                    logger.warn("PostgreSQL: Found stale PREPARED task {}, deleting it", taskId);
                    repository.delete(existingTask);
                }
            });

            // Create and save task with PREPARED status
            // This is persisted to database immediately (within transaction)
            BatchHealthCheckTask task = new BatchHealthCheckTask(taskId, serviceNames);
            task.setStatus(TaskStatus.PREPARED);
            BatchHealthCheckTask saved = repository.save(task);

            // Store mapping for commit/rollback
            transactionToTaskId.put(transactionId, taskId);

            logger.info("PostgreSQL: Prepared transaction {} successfully, task saved with ID {}",
                    transactionId, saved.getId());
            return true;
        } catch (DataIntegrityViolationException e) {
            // Task with same taskId already exists (concurrent transaction)
            logger.error("PostgreSQL: Failed to prepare transaction {} - task {} already exists", transactionId, taskId, e);
            return false;
        } catch (Exception e) {
            logger.error("PostgreSQL: Failed to prepare transaction {}", transactionId, e);
            return false;
        }
    }

    /**
     * Phase 2: Commit - Update task status to COMPLETED.
     * <p>
     * Updates the prepared task to COMPLETED status.
     * Uses database transaction to ensure atomicity.
     */
    @Override
    @Transactional
    public boolean commit(String transactionId) {
        try {
            logger.info("PostgreSQL: Committing transaction {}", transactionId);
            String taskId = transactionToTaskId.get(transactionId);
            if (taskId == null) {
                logger.error("PostgreSQL: No task ID found for transaction {}", transactionId);
                return false;
            }

            // Find and update the prepared task
            BatchHealthCheckTask task = repository.findByTaskId(taskId).orElseThrow(() -> new IllegalStateException("Task not found for commit: " + taskId));

            if (task.getStatus() != TaskStatus.PREPARED) {
                logger.error("PostgreSQL: Task {} is not in PREPARED status, current status: {}", taskId, task.getStatus());
                return false;
            }

            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            repository.save(task);
            transactionToTaskId.remove(transactionId);

            logger.info("PostgreSQL: Committed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("PostgreSQL: Failed to commit transaction {}", transactionId, e);
            return false;
        }
    }

    /**
     * Rollback - Delete the prepared task from database.
     * <p>
     * Removes the prepared task record completely.
     * Uses database transaction to ensure atomicity.
     */
    @Override
    @Transactional
    public void rollback(String transactionId) {
        try {
            logger.info("PostgreSQL: Rolling back transaction {}", transactionId);
            String taskId = transactionToTaskId.remove(transactionId);
            if (taskId == null) {
                logger.warn("PostgreSQL: No task ID found for rollback transaction {}", transactionId);
                return;
            }

            // Find and delete the prepared task
            repository.findByTaskId(taskId).ifPresentOrElse(
                    task -> {
                        if (task.getStatus() == TaskStatus.PREPARED) {
                            repository.delete(task);
                            logger.info("PostgreSQL: Rolled back transaction {} - deleted prepared task {}",
                                    transactionId, taskId);
                        } else {
                            // Task was already committed or in different state
                            logger.warn("PostgreSQL: Task {} is not in PREPARED status during rollback, " +
                                    "current status: {}", taskId, task.getStatus());
                        }
                    },
                    () -> logger.warn("PostgreSQL: Task {} not found for rollback", taskId)
            );
        } catch (Exception e) {
            logger.error("PostgreSQL: Failed to rollback transaction {}", transactionId, e);
        }
    }
}
