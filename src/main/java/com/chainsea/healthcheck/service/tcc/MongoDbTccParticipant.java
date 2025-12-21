package com.chainsea.healthcheck.service.tcc;

import com.chainsea.healthcheck.model.TaskStatus;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

/**
 * MongoDB participant for TCC pattern.
 * Try: Insert log with "RESERVED" status
 * Confirm: Update status to "COMPLETED"
 * Cancel: Delete the reserved document
 */
@Component
public class MongoDbTccParticipant implements TccParticipant {

    private static final Logger logger = LoggerFactory.getLogger(MongoDbTccParticipant.class);
    private static final String COLLECTION = "batch_task_logs";

    private final MongoTemplate mongoTemplate;
    private final Map<String, String> reservedDocumentIds = new ConcurrentHashMap<>();

    public MongoDbTccParticipant(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean tryExecute(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("MongoDB TCC: Trying transaction {}", transactionId);
            Document logDoc = new Document();
            logDoc.append("transactionId", transactionId);
            logDoc.append("taskId", taskId);
            logDoc.append("serviceNames", serviceNames);
            logDoc.append("status", TaskStatus.RESERVED);
            logDoc.append("createdAt", Instant.now());

            Document saved = mongoTemplate.insert(logDoc, COLLECTION);
            String documentId = saved.getObjectId("_id").toString();
            reservedDocumentIds.put(transactionId, documentId);

            logger.info("MongoDB TCC: Tried transaction {} successfully, document ID: {}", transactionId, documentId);
            return true;
        } catch (Exception e) {
            logger.error("MongoDB TCC: Failed to try transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean confirm(String transactionId) {
        try {
            logger.info("MongoDB TCC: Confirming transaction {}", transactionId);
            String documentId = reservedDocumentIds.get(transactionId);
            if (documentId == null) {
                logger.error("MongoDB TCC: No reserved document found for transaction {}", transactionId);
                return false;
            }

            mongoTemplate.updateFirst(
                    query(where("_id").is(new ObjectId(documentId))), update("status", TaskStatus.COMPLETED).set("completedAt", Instant.now()),
                    COLLECTION);

            reservedDocumentIds.remove(transactionId);
            logger.info("MongoDB TCC: Confirmed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("MongoDB TCC: Failed to confirm transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void cancel(String transactionId) {
        try {
            logger.info("MongoDB TCC: Cancelling transaction {}", transactionId);
            String documentId = reservedDocumentIds.remove(transactionId);
            if (documentId != null) {
                mongoTemplate.remove(query(where("_id").is(new ObjectId(documentId))), COLLECTION);
            }
            logger.info("MongoDB TCC: Cancelled transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("MongoDB TCC: Failed to cancel transaction {}", transactionId, e);
        }
    }
}
