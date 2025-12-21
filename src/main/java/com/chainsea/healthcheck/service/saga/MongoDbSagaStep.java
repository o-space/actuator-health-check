package com.chainsea.healthcheck.service.saga;

import com.chainsea.healthcheck.model.TaskStatus;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

/**
 * MongoDB step for Saga pattern.
 * Execute: Log task details to MongoDB (local transaction)
 * Compensate: Delete the log document
 */
@Component
public class MongoDbSagaStep implements SagaStep {

    private static final Logger logger = LoggerFactory.getLogger(MongoDbSagaStep.class);
    private static final String COLLECTION = "batch_task_logs";
    private static final String STEP_NAME = "MongoDB";

    private final MongoTemplate mongoTemplate;

    public MongoDbSagaStep(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String getStepName() {
        return STEP_NAME;
    }

    @Override
    public boolean execute(SagaContext sagaContext, String taskId, List<String> serviceNames) {
        try {
            logger.info("MongoDB Saga: Executing step for task {}", taskId);
            Document logDoc = new Document();
            logDoc.append("sagaId", sagaContext.getSagaId());
            logDoc.append("taskId", taskId);
            logDoc.append("serviceNames", serviceNames);
            logDoc.append("status", TaskStatus.COMPLETED);
            logDoc.append("createdAt", Instant.now());

            // Execute local transaction - insert immediately
            Document saved = mongoTemplate.insert(logDoc, COLLECTION);
            String documentId = saved.getObjectId("_id").toString();

            // Store document ID in context for compensation
            sagaContext.addStepData(STEP_NAME, documentId);
            sagaContext.addStepResult(STEP_NAME, saved);

            logger.info("MongoDB Saga: Task {} logged with document ID {}", taskId, documentId);
            return true;
        } catch (Exception e) {
            logger.error("MongoDB Saga: Failed to execute step for task {}", taskId, e);
            return false;
        }
    }

    @Override
    public void compensate(SagaContext sagaContext) {
        try {
            logger.info("MongoDB Saga: Compensating step");
            String documentId = (String) sagaContext.getStepData(STEP_NAME);
            if (documentId != null) {
                mongoTemplate.remove(query(where("_id").is(new ObjectId(documentId))), COLLECTION);
                logger.info("MongoDB Saga: Log document {} deleted", documentId);
            }
        } catch (Exception e) {
            logger.error("MongoDB Saga: Failed to compensate", e);
        }
    }
}
