package com.sanddollar.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Profiles;

public class PlaidDataSourceCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        if (environment != null && environment.acceptsProfiles(Profiles.of("plaid"))) {
            return true;
        }
        String dataSource = environment != null ? environment.getProperty("APP_DATA_SOURCE") : null;
        if (dataSource == null || dataSource.isBlank()) {
            dataSource = environment != null ? environment.getProperty("data-source-mode") : null;
        }
        return dataSource != null && dataSource.equalsIgnoreCase("plaid");
    }
}
