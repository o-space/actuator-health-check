package com.chainsea.healthcheck.service.twophase;

import com.chainsea.healthcheck.model.BatchHealthCheckTask;
import com.chainsea.healthcheck.model.TaskStatus;
import com.chainsea.healthcheck.repository.BatchHealthCheckTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL participant for 2PC protocol.
 * Manages task records in the database.
 */
@Component
public class PostgresParticipant implements TwoPhaseCommitParticipant {

    private static final Logger logger = LoggerFactory.getLogger(PostgresParticipant.class);

    private final BatchHealthCheckTaskRepository repository;
    private final Map<String, BatchHealthCheckTask> preparedTasks = new ConcurrentHashMap<>();

    public PostgresParticipant(BatchHealthCheckTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean prepare(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("PostgreSQL: Preparing transaction {}", transactionId);
            // Create task but don't save yet - store in memory
            BatchHealthCheckTask task = new BatchHealthCheckTask(taskId, serviceNames);
            task.setStatus(TaskStatus.PROCESSING);
            preparedTasks.put(transactionId, task);
            logger.info("PostgreSQL: Prepared transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("PostgreSQL: Failed to prepare transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean commit(String transactionId) {
        try {
            logger.info("PostgreSQL: Committing transaction {}", transactionId);
            BatchHealthCheckTask task = preparedTasks.get(transactionId);
            if (task == null) {
                logger.error("PostgreSQL: No prepared task found for transaction {}", transactionId);
                return false;
            }
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            repository.save(task);
            preparedTasks.remove(transactionId);
            logger.info("PostgreSQL: Committed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("PostgreSQL: Failed to commit transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void rollback(String transactionId) {
        try {
            logger.info("PostgreSQL: Rolling back transaction {}", transactionId);
            BatchHealthCheckTask task = preparedTasks.remove(transactionId);
            if (task != null && task.getId() != null) {
                task.setStatus(TaskStatus.FAILED);
                repository.save(task);
            }

            logger.info("PostgreSQL: Rolled back transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("PostgreSQL: Failed to rollback transaction {}", transactionId, e);
        }
    }
}
