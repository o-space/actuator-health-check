package com.chainsea.healthcheck.service.twophase;

import com.chainsea.healthcheck.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ participant for 2PC protocol.
 * Manages task notifications via message queue.
 */
@Component
public class RabbitMqParticipant implements TwoPhaseCommitParticipant {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqParticipant.class);
    private static final String EXCHANGE = "healthcheck.exchange";
    private static final String ROUTING_KEY = "batch.task";

    // Note: Exchange and queue should be configured via RabbitMqConfig

    private final RabbitTemplate rabbitTemplate;
    private final Map<String, MessageData> preparedMessages = new ConcurrentHashMap<>();

    public RabbitMqParticipant(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public boolean prepare(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("RabbitMQ: Preparing transaction {}", transactionId);
            // Prepare message but don't send yet
            MessageData messageData = new MessageData(taskId, serviceNames, TaskStatus.PROCESSING);
            preparedMessages.put(transactionId, messageData);
            logger.info("RabbitMQ: Prepared transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("RabbitMQ: Failed to prepare transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean commit(String transactionId) {
        try {
            logger.info("RabbitMQ: Committing transaction {}", transactionId);
            MessageData messageData = preparedMessages.get(transactionId);
            if (messageData == null) {
                logger.error("RabbitMQ: No prepared message found for transaction {}", transactionId);
                return false;
            }

            // Actually send the message
            messageData.setStatus(TaskStatus.COMPLETED);
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, messageData);
            preparedMessages.remove(transactionId);

            logger.info("RabbitMQ: Committed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("RabbitMQ: Failed to commit transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void rollback(String transactionId) {
        try {
            logger.info("RabbitMQ: Rolling back transaction {}", transactionId);
            preparedMessages.remove(transactionId);
            logger.info("RabbitMQ: Rolled back transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("RabbitMQ: Failed to rollback transaction {}", transactionId, e);
        }
    }

    private static class MessageData implements Serializable {
        private static final long serialVersionUID = 1L;

        private String taskId;
        private List<String> serviceNames;
        private TaskStatus status;

        public MessageData(String taskId, List<String> serviceNames, TaskStatus status) {
            this.taskId = taskId;
            this.serviceNames = serviceNames;
            this.status = status;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public List<String> getServiceNames() {
            return serviceNames;
        }

        public void setServiceNames(List<String> serviceNames) {
            this.serviceNames = serviceNames;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public void setStatus(TaskStatus status) {
            this.status = status;
        }
    }
}
