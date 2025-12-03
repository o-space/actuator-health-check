package com.chainsea.healthcheck.health;

import com.chainsea.healthcheck.config.ConditionalOnServiceConfigured;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component("redis")
@ConditionalOnServiceConfigured("redis")
public class RedisHealthIndicator extends AbstractHealthIndicator {

    private final StringRedisTemplate redisTemplate;

    public RedisHealthIndicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        // TODO: timeout config (!important)
        String pong = redisTemplate.execute(RedisConnectionCommands::ping, false);
        if ("PONG".equalsIgnoreCase(pong)) {
            builder.up();
        } else {
            builder.down().withDetail("error", "Unexpected ping response");
        }
    }
}
