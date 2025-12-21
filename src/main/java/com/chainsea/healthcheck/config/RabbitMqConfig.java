package com.chainsea.healthcheck.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ's configuration for batch health check notifications.
 */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "healthcheck.exchange";
    public static final String QUEUE = "batch.task.queue";
    public static final String ROUTING_KEY = "batch.task";

    @Bean
    public DirectExchange healthcheckExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue batchTaskQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding batchTaskBinding() {
        return BindingBuilder.bind(batchTaskQueue())
                .to(healthcheckExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
