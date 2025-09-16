package com.sanddollar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaidConfig {

    @Value("${plaid.client-id:}")
    private String clientId;

    @Value("${plaid.secret:}")
    private String secret;

    @Value("${plaid.environment:sandbox}")
    private String environment;

    @Value("${plaid.redirect-uri:}")
    private String redirectUri;

    public String getClientId() {
        return clientId;
    }

    public String getSecret() {
        return secret;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    // Configuration placeholder - actual Plaid client would be configured here in production
    // For now, the SandboxDevController generates mock data
}
