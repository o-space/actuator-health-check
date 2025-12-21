package com.chainsea.healthcheck.service.twophase;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MongoDB participant for 2PC protocol.
 * Manages task logs in MongoDB.
 */
@Component
public class MongoDbParticipant implements TwoPhaseCommitParticipant {

    private static final Logger logger = LoggerFactory.getLogger(MongoDbParticipant.class);
    private static final String COLLECTION = "batch_task_logs";
    private static final String PREPARE_COLLECTION = "batch_task_logs_prepare";

    private final MongoTemplate mongoTemplate;
    private final Map<String, Document> preparedDocuments = new ConcurrentHashMap<>();

    public MongoDbParticipant(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean prepare(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("MongoDB: Preparing transaction {}", transactionId);
            Document logDoc = new Document();
            logDoc.append("transactionId", transactionId);
            logDoc.append("taskId", taskId);
            logDoc.append("serviceNames", serviceNames);
            logDoc.append("status", "PROCESSING");
            logDoc.append("createdAt", Instant.now());

            // Store in prepare collection (temporary)
            mongoTemplate.insert(logDoc, PREPARE_COLLECTION);
            preparedDocuments.put(transactionId, logDoc);

            logger.info("MongoDB: Prepared transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("MongoDB: Failed to prepare transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean commit(String transactionId) {
        try {
            logger.info("MongoDB: Committing transaction {}", transactionId);
            Document logDoc = preparedDocuments.get(transactionId);
            if (logDoc == null) {
                logger.error("MongoDB: No prepared document found for transaction {}", transactionId);
                return false;
            }

            // Move from prepare collection to actual collection
            logDoc.append("status", "COMPLETED");
            logDoc.append("completedAt", Instant.now());
            mongoTemplate.insert(logDoc, COLLECTION);
            mongoTemplate.remove(logDoc, PREPARE_COLLECTION);
            preparedDocuments.remove(transactionId);

            logger.info("MongoDB: Committed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("MongoDB: Failed to commit transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void rollback(String transactionId) {
        try {
            logger.info("MongoDB: Rolling back transaction {}", transactionId);
            Document logDoc = preparedDocuments.remove(transactionId);
            if (logDoc != null) {
                mongoTemplate.remove(logDoc, PREPARE_COLLECTION);
            }
            logger.info("MongoDB: Rolled back transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("MongoDB: Failed to rollback transaction {}", transactionId, e);
        }
    }
}
