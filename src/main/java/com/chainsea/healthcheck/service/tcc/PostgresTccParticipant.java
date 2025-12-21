package com.chainsea.healthcheck.service.tcc;

import com.chainsea.healthcheck.model.BatchHealthCheckTask;
import com.chainsea.healthcheck.model.TaskStatus;
import com.chainsea.healthcheck.repository.BatchHealthCheckTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL participant for TCC pattern.
 * Try: Create task with "RESERVED" status
 * Confirm: Change status to "COMPLETED"
 * Cancel: Delete or mark as "CANCELLED"
 */
@Component
public class PostgresTccParticipant implements TccParticipant {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTccParticipant.class);

    private final BatchHealthCheckTaskRepository repository;
    private final Map<String, Long> reservedTaskIds = new ConcurrentHashMap<>();

    public PostgresTccParticipant(BatchHealthCheckTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean tryExecute(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("PostgreSQL TCC: Trying transaction {}", transactionId);
            // Create task with RESERVED status (not committed yet)
            BatchHealthCheckTask task = new BatchHealthCheckTask(taskId, serviceNames);
            task.setStatus(TaskStatus.RESERVED);
            BatchHealthCheckTask saved = repository.save(task);
            reservedTaskIds.put(transactionId, saved.getId());
            logger.info("PostgreSQL TCC: Tried transaction {} successfully, task ID: {}", transactionId, saved.getId());
            return true;
        } catch (Exception e) {
            logger.error("PostgreSQL TCC: Failed to try transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean confirm(String transactionId) {
        try {
            logger.info("PostgreSQL TCC: Confirming transaction {}", transactionId);
            Long taskId = reservedTaskIds.get(transactionId);
            if (taskId == null) {
                logger.error("PostgreSQL TCC: No reserved task found for transaction {}", transactionId);
                return false;
            }

            BatchHealthCheckTask task = repository.findById(taskId).orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));
            task.setStatus(TaskStatus.COMPLETED);
            task.setCompletedAt(java.time.LocalDateTime.now());
            repository.save(task);
            reservedTaskIds.remove(transactionId);

            logger.info("PostgreSQL TCC: Confirmed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("PostgreSQL TCC: Failed to confirm transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void cancel(String transactionId) {
        try {
            logger.info("PostgreSQL TCC: Cancelling transaction {}", transactionId);
            Long taskId = reservedTaskIds.remove(transactionId);
            if (taskId != null) {
                BatchHealthCheckTask task = repository.findById(taskId).orElse(null);
                if (task != null) {
                    task.setStatus(TaskStatus.CANCELLED);
                    repository.save(task);
                }
            }
            logger.info("PostgreSQL TCC: Cancelled transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("PostgreSQL TCC: Failed to cancel transaction {}", transactionId, e);
        }
    }
}
