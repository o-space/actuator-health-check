package com.chainsea.healthcheck.service.tcc;

import java.util.List;

/**
 * Interface for TCC participants.
 * Each participant must implement try, confirm, and cancel operations.
 */
public interface TccParticipant {

    /**
     * Try phase: Reserve resources but don't commit yet.
     * This should perform validation and reserve resources (e.g., lock, temporary record).
     *
     * @param transactionId the transaction ID
     * @param taskId        the task ID
     * @param serviceNames  list of service names
     * @return true if try succeeded, false otherwise
     */
    boolean tryExecute(String transactionId, String taskId, List<String> serviceNames);

    /**
     * Confirm phase: Actually commit the reserved resources.
     *
     * @param transactionId the transaction ID
     * @return true if confirmed successfully, false otherwise
     */
    boolean confirm(String transactionId);

    /**
     * Cancel phase: Release reserved resources.
     *
     * @param transactionId the transaction ID
     */
    void cancel(String transactionId);
}
