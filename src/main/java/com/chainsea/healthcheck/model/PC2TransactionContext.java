package com.chainsea.healthcheck.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for distributed transaction coordination.
 */
public class PC2TransactionContext {
    private final String transactionId;
    private final Map<String, Object> participants = new ConcurrentHashMap<>();
    private TransactionPhase currentPhase = TransactionPhase.INITIALIZED;

    public PC2TransactionContext(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Map<String, Object> getParticipants() {
        return participants;
    }

    public void addParticipant(String participantId, Object participant) {
        participants.put(participantId, participant);
    }

    public TransactionPhase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(TransactionPhase currentPhase) {
        this.currentPhase = currentPhase;
    }

    public enum TransactionPhase {
        INITIALIZED,
        PREPARING,
        PREPARED,
        COMMITTING,
        COMMITTED,
        ROLLING_BACK,
        ROLLED_BACK
    }
}
