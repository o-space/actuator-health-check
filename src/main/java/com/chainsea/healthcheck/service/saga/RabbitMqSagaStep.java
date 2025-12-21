package com.chainsea.healthcheck.service.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * RabbitMQ step for Saga pattern.
 * Execute: Send notification message (local transaction)
 * Compensate: Send compensation message (optional, as message is already sent)
 * Note: Message sending is typically not compensatable, but we can send a cancellation message
 */
@Component
public class RabbitMqSagaStep implements SagaStep {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMqSagaStep.class);
    private static final String EXCHANGE = "healthcheck.exchange";
    private static final String ROUTING_KEY = "batch.task";
    private static final String COMPENSATION_ROUTING_KEY = "batch.task.cancel";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqSagaStep(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public String getStepName() {
        return "RabbitMQ";
    }

    @Override
    public boolean execute(SagaContext sagaContext, String taskId, List<String> serviceNames) {
        try {
            logger.info("RabbitMQ Saga: Executing step for task {}", taskId);
            // Execute local transaction - send message immediately
            MessageData messageData = new MessageData(taskId, serviceNames, "PROCESSING");
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, messageData);

            // Store message data in context for compensation
            sagaContext.addStepData("rabbitmq", messageData);
            sagaContext.addStepResult("rabbitmq", "SENT");

            logger.info("RabbitMQ Saga: Notification sent for task {}", taskId);
            return true;
        } catch (Exception e) {
            logger.error("RabbitMQ Saga: Failed to execute step for task {}", taskId, e);
            return false;
        }
    }

    @Override
    public void compensate(SagaContext sagaContext) {
        try {
            logger.info("RabbitMQ Saga: Compensating step");
            MessageData messageData = (MessageData) sagaContext.getStepData("rabbitmq");
            if (messageData != null) {
                // Send a cancellation message to notify downstream services
                messageData.setStatus("CANCELLED");
                rabbitTemplate.convertAndSend(EXCHANGE, COMPENSATION_ROUTING_KEY, messageData);
                logger.info("RabbitMQ Saga: Cancellation message sent for task {}", messageData.getTaskId());
            }
        } catch (Exception e) {
            logger.error("RabbitMQ Saga: Failed to compensate", e);
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
