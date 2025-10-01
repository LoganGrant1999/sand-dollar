package com.sanddollar.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleClientSecretGenerator {

    @Value("${APPLE_TEAM_ID:}")
    private String teamId;

    @Value("${APPLE_KEY_ID:}")
    private String keyId;

    @Value("${APPLE_SERVICE_ID:}")
    private String serviceId;

    @Value("${APPLE_P8:}")
    private String p8Content;

    public boolean isAppleConfigured() {
        return !teamId.isEmpty() && !keyId.isEmpty() && !serviceId.isEmpty() && !p8Content.isEmpty();
    }

    public String generateClientSecret() {
        if (!isAppleConfigured()) {
            throw new IllegalStateException("Apple OAuth configuration is not complete. " +
                "Please set APPLE_TEAM_ID, APPLE_KEY_ID, APPLE_SERVICE_ID, and APPLE_P8 environment variables.");
        }

        try {
            // Parse the P8 private key
            PrivateKey privateKey = parsePrivateKey(p8Content);

            // Create JWT for Apple client secret
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(3600); // 1 hour expiration

            return Jwts.builder()
                    .setIssuer(teamId)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiration))
                    .setAudience("https://appleid.apple.com")
                    .setSubject(serviceId)
                    .setHeaderParam("kid", keyId)
                    .setHeaderParam("alg", "ES256")
                    .signWith(privateKey, SignatureAlgorithm.ES256)
                    .compact();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Apple client secret", e);
        }
    }

    private PrivateKey parsePrivateKey(String p8Content) throws Exception {
        // Remove PEM headers and whitespace
        String cleanedP8 = p8Content
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanedP8);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(keySpec);
    }
}