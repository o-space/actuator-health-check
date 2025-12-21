package com.chainsea.healthcheck.service.saga;

import java.util.List;

/**
 * Represents a step in a Saga transaction.
 * Each step has an execute action and a compensate action.
 */
public interface SagaStep {

    /**
     * Execute the step (local transaction).
     *
     * @param sagaContext  the saga context
     * @param taskId       the task ID
     * @param serviceNames list of service names
     * @return true if executed successfully, false otherwise
     */
    boolean execute(SagaContext sagaContext, String taskId, List<String> serviceNames);

    /**
     * Compensate (undo) the step if a later step fails.
     *
     * @param sagaContext the saga context
     */
    void compensate(SagaContext sagaContext);

    /**
     * Get the step name for logging and identification.
     *
     * @return step name
     */
    String getStepName();
}
