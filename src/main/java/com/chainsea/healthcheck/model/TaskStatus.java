package com.chainsea.healthcheck.model;

/**
 * Status of a batch health check task.
 */
public enum TaskStatus {
    /**
     * Task is pending and not yet started.
     */
    PENDING,

    /**
     * Task is currently being processed.
     */
    PROCESSING,

    /**
     * Task has been prepared (2PC pattern - Prepare phase).
     * Data is persisted but transaction is not yet committed.
     */
    PREPARED,

    /**
     * Task has been reserved (TCC pattern - Try phase).
     */
    RESERVED,

    /**
     * Task completed successfully.
     */
    COMPLETED,

    /**
     * Task failed during execution.
     */
    FAILED,

    /**
     * Task was cancelled (TCC pattern - Cancel phase).
     */
    CANCELLED
}
