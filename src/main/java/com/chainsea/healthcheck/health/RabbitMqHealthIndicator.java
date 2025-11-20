package com.chainsea.healthcheck.health;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component("rabbitmq")
public class RabbitMqHealthIndicator extends AbstractHealthIndicator {

    private final ConnectionFactory connectionFactory;

    public RabbitMqHealthIndicator(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try (var connection = connectionFactory.createConnection()) {
            if (connection.isOpen()) {
                builder.up();
                return;
            }
            builder.down().withDetail("error", "Connection not open");
        } catch (Exception ex) {
            builder.down(ex);
        }
    }
}

