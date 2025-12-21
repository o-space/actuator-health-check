package com.chainsea.healthcheck.service.twophase;

import java.util.List;

/**
 * Interface for 2PC participants.
 * Each participant must implement prepare, commit, and rollback operations.
 */
public interface TwoPhaseCommitParticipant {

    /**
     * Phase 1: Prepare - Check if the participant can commit.
     * This should perform validation and prepare resources but not commit yet.
     *
     * @param transactionId the transaction ID
     * @param taskId        the task ID
     * @param serviceNames  list of service names
     * @return true if prepared successfully, false otherwise
     */
    boolean prepare(String transactionId, String taskId, List<String> serviceNames);

    /**
     * Phase 2: Commit - Actually commit the changes.
     *
     * @param transactionId the transaction ID
     * @return true if committed successfully, false otherwise
     */
    boolean commit(String transactionId);

    /**
     * Rollback - Undo any changes made during prepare phase.
     *
     * @param transactionId the transaction ID
     */
    void rollback(String transactionId);
}
