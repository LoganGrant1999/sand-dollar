package com.sanddollar.config;

import com.plaid.client.ApiClient;
import com.plaid.client.auth.ApiKeyAuth;
import com.plaid.client.request.PlaidApi;
import okhttp3.Interceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Configuration
@Profile("plaid")
public class PlaidClientConfig {

    private static final String PLAID_VERSION = "2020-09-14";

    @Bean
    public PlaidApi plaidApi(PlaidConfig plaidConfig) {
        ApiClient apiClient = new ApiClient(new String[]{"clientId", "secret", "plaidVersion"});
        apiClient.setPlaidAdapter(resolveBaseUrl(plaidConfig.getEnvironment()));

        Map<String, Interceptor> auths = apiClient.getApiAuthorizations();
        ApiKeyAuth clientIdAuth = (ApiKeyAuth) auths.get("clientId");
        ApiKeyAuth secretAuth = (ApiKeyAuth) auths.get("secret");
        ApiKeyAuth versionAuth = (ApiKeyAuth) auths.get("plaidVersion");

        if (clientIdAuth != null) {
            clientIdAuth.setApiKey(plaidConfig.getClientId());
        }
        if (secretAuth != null) {
            secretAuth.setApiKey(plaidConfig.getSecret());
        }
        if (versionAuth != null) {
            versionAuth.setApiKey(PLAID_VERSION);
        }

        return apiClient.createService(PlaidApi.class);
    }

    private String resolveBaseUrl(String environment) {
        if (environment == null) {
            return ApiClient.Sandbox;
        }
        return switch (environment.toLowerCase()) {
            case "production" -> ApiClient.Production;
            case "development", "development-sandbox", "dev" -> ApiClient.Development;
            default -> ApiClient.Sandbox;
        };
    }
}
