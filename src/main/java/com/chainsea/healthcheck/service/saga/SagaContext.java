package com.chainsea.healthcheck.service.saga;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for Saga transaction coordination.
 * Stores execution results and state for compensation.
 */
public class SagaContext {
    private final String sagaId;
    private final Map<String, Object> stepResults = new ConcurrentHashMap<>();
    private final Map<String, Object> stepData = new ConcurrentHashMap<>();
    private int currentStepIndex = -1;

    public SagaContext(String sagaId) {
        this.sagaId = sagaId;
    }

    public String getSagaId() {
        return sagaId;
    }

    public Map<String, Object> getStepResults() {
        return stepResults;
    }

    public void addStepResult(String stepName, Object result) {
        stepResults.put(stepName, result);
    }

    public Object getStepResult(String stepName) {
        return stepResults.get(stepName);
    }

    public Map<String, Object> getStepData() {
        return stepData;
    }

    public void addStepData(String stepName, Object data) {
        stepData.put(stepName, data);
    }

    public Object getStepData(String stepName) {
        return stepData.get(stepName);
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public void setCurrentStepIndex(int currentStepIndex) {
        this.currentStepIndex = currentStepIndex;
    }
}
