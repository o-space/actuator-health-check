package com.chainsea.healthcheck.health;

import org.bson.Document;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component("mongodb")
public class MongoDbHealthIndicator extends AbstractHealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoDbHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            Document result = mongoTemplate.getDb().runCommand(Document.parse("{ ping: 1 }"));
            Object ok = result.get("ok");
            if (ok instanceof Number && ((Number) ok).doubleValue() == 1.0) {
                builder.up();
            } else {
                builder.down().withDetail("error", "Unexpected ping response: " + ok);
            }
        } catch (Exception ex) {
            builder.down(ex);
        }
    }
}

