package com.chainsea.healthcheck.health;

import com.chainsea.healthcheck.config.ConditionalOnServiceConfigured;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component("postgres")
@ConditionalOnServiceConfigured("postgres")
public class PostgresHealthIndicator extends AbstractHealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public PostgresHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        if (result != null && result == 1) {
            builder.up();
        } else {
            builder.down().withDetail("error", "Unexpected response from database");
        }
    }
}

