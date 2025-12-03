package com.chainsea.healthcheck.health;

import com.chainsea.healthcheck.config.ConditionalOnServiceConfigured;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.stereotype.Component;

@Component("rabbitmq")
@ConditionalOnServiceConfigured("rabbitmq")
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
            resetIfCachingFactory();
            builder.down().withDetail("error", "Connection not open");
        } catch (AmqpException ex) {
            resetIfCachingFactory();
            builder.down(ex);
        }
    }

    // Note: Do not put connection reset in health check, put it in logic
    private void resetIfCachingFactory() {
        if (connectionFactory instanceof CachingConnectionFactory cachingConnectionFactory) {
            cachingConnectionFactory.resetConnection();
        }
    }
}

