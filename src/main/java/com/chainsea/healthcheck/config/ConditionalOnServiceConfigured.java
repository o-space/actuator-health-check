package com.chainsea.healthcheck.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnServiceConfiguredCondition.class)
public @interface ConditionalOnServiceConfigured {
    String value();
}

class OnServiceConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String serviceName = metadata.getAnnotationAttributes(ConditionalOnServiceConfigured.class.getName()).get("value").toString();

        try {
            Environment environment = context.getEnvironment();
            if (environment == null) {
                return false;
            }

            // 从配置中读取所有服务名称
            List<String> allServiceNames = new ArrayList<>();

            int criticalIndex = 0;
            while (true) {
                String name = environment.getProperty("health-check.critical-services[" + criticalIndex + "].name");
                if (name == null) {
                    break;
                }
                allServiceNames.add(name);
                criticalIndex++;
            }

            int nonCriticalIndex = 0;
            while (true) {
                String name = environment.getProperty("health-check.non-critical-services[" + nonCriticalIndex + "].name");
                if (name == null) {
                    break;
                }
                allServiceNames.add(name);
                nonCriticalIndex++;
            }

            return allServiceNames.contains(serviceName);
        } catch (Exception e) {
            return false;
        }
    }
}

