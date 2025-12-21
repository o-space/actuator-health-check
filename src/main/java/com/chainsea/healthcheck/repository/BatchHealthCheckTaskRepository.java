package com.chainsea.healthcheck.repository;

import com.chainsea.healthcheck.model.BatchHealthCheckTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for batch health check tasks.
 */
@Repository
public interface BatchHealthCheckTaskRepository extends JpaRepository<BatchHealthCheckTask, Long> {
    Optional<BatchHealthCheckTask> findByTaskId(String taskId);
}
