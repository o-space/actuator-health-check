package com.chainsea.healthcheck.service.tcc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RabbitMQ participant for TCC pattern.
 * Try: Prepare message but don't send (store in memory)
 * Confirm: Actually send the message
 * Cancel: Discard the prepared message
 */
@Component
public class RabbitMqTccParticipant implements TccParticipant {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqTccParticipant.class);
    private static final String EXCHANGE = "healthcheck.exchange";
    private static final String ROUTING_KEY = "batch.task";

    // Note: Exchange and queue should be configured via RabbitMqConfig

    private final RabbitTemplate rabbitTemplate;
    private final Map<String, MessageData> reservedMessages = new ConcurrentHashMap<>();

    public RabbitMqTccParticipant(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public boolean tryExecute(String transactionId, String taskId, List<String> serviceNames) {
        try {
            logger.info("RabbitMQ TCC: Trying transaction {}", transactionId);
            // Prepare message but don't send yet
            MessageData messageData = new MessageData(taskId, serviceNames, "RESERVED");
            reservedMessages.put(transactionId, messageData);
            logger.info("RabbitMQ TCC: Tried transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("RabbitMQ TCC: Failed to try transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public boolean confirm(String transactionId) {
        try {
            logger.info("RabbitMQ TCC: Confirming transaction {}", transactionId);
            MessageData messageData = reservedMessages.get(transactionId);
            if (messageData == null) {
                logger.error("RabbitMQ TCC: No reserved message found for transaction {}", transactionId);
                return false;
            }

            // Actually send the message
            messageData.setStatus("COMPLETED");
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, messageData);
            reservedMessages.remove(transactionId);

            logger.info("RabbitMQ TCC: Confirmed transaction {} successfully", transactionId);
            return true;
        } catch (Exception e) {
            logger.error("RabbitMQ TCC: Failed to confirm transaction {}", transactionId, e);
            return false;
        }
    }

    @Override
    public void cancel(String transactionId) {
        try {
            logger.info("RabbitMQ TCC: Cancelling transaction {}", transactionId);
            reservedMessages.remove(transactionId);
            logger.info("RabbitMQ TCC: Cancelled transaction {} successfully", transactionId);
        } catch (Exception e) {
            logger.error("RabbitMQ TCC: Failed to cancel transaction {}", transactionId, e);
        }
    }

    private static class MessageData implements Serializable {
        private static final long serialVersionUID = 1L;

        private String taskId;
        private List<String> serviceNames;
        private String status;

        public MessageData(String taskId, List<String> serviceNames, String status) {
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
