package com.chainsea.healthcheck.controller;

import com.chainsea.healthcheck.controller.dto.BatchHealthCheckRequest;
import com.chainsea.healthcheck.service.saga.SagaOrchestrator;
import com.chainsea.healthcheck.service.tcc.TccCoordinator;
import com.chainsea.healthcheck.service.twophase.TwoPhaseCommitCoordinator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for batch health check operations with distributed transaction support.
 */
@RestController
@RequestMapping("/api/batch-health-checks")
public class BatchHealthCheckController {

    private final TwoPhaseCommitCoordinator twoPhaseCommitCoordinator;
    private final TccCoordinator tccCoordinator;
    private final SagaOrchestrator sagaOrchestrator;

    public BatchHealthCheckController(
            TwoPhaseCommitCoordinator twoPhaseCommitCoordinator,
            TccCoordinator tccCoordinator,
            SagaOrchestrator sagaOrchestrator) {
        this.twoPhaseCommitCoordinator = twoPhaseCommitCoordinator;
        this.tccCoordinator = tccCoordinator;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    private static Map<String, Object> generateResponse(String taskId, String v2, boolean success, String message) {
        return Map.of(
                "taskId", taskId,
                "pattern", v2,
                "success", success,
                "message", message
        );
    }

    /**
     * Create a batch health check task using 2PC protocol.
     *
     * @param request the batch health check request
     * @return response with transaction result
     */
    @PostMapping("/2pc")
    public ResponseEntity<Map<String, Object>> createBatchTaskWith2PC(@RequestBody BatchHealthCheckRequest request) {
        boolean success = twoPhaseCommitCoordinator.executeTransaction(request.taskId(), request.serviceNames());
        Map<String, Object> response = generateResponse(request.taskId(), "2PC", success, success ? "Transaction committed successfully" : "Transaction failed and rolled back");
        return success ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);
    }

    /**
     * Create a batch health check task using TCC pattern.
     *
     * @param request the batch health check request
     * @return response with transaction result
     */
    @PostMapping("/tcc")
    public ResponseEntity<Map<String, Object>> createBatchTaskWithTCC(@RequestBody BatchHealthCheckRequest request) {
        boolean success = tccCoordinator.executeTransaction(request.taskId(), request.serviceNames());
        Map<String, Object> response = generateResponse(request.taskId(), "TCC", success, success ? "Transaction confirmed successfully" : "Transaction failed and cancelled");
        return success ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);
    }

    /**
     * Create a batch health check task using Saga pattern.
     *
     * @param request the batch health check request
     * @return response with transaction result
     */
    @PostMapping("/saga")
    public ResponseEntity<Map<String, Object>> createBatchTaskWithSaga(@RequestBody BatchHealthCheckRequest request) {
        boolean success = sagaOrchestrator.executeSaga(request.taskId(), request.serviceNames());
        Map<String, Object> response = generateResponse(request.taskId(), "Saga", success, success ? "Saga transaction completed successfully" : "Saga transaction failed and compensated");
        return success ? ResponseEntity.ok(response) : ResponseEntity.status(500).body(response);
    }

}
