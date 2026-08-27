package com.carebridge.backend.file.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "carebridge.storage.private-provider", havingValue = "r2")
public class R2StorageConfig {

    @Bean
    S3Client r2S3Client(
            @Value("${carebridge.storage.r2.endpoint}") String endpoint,
            @Value("${carebridge.storage.r2.access-key}") String accessKey,
            @Value("${carebridge.storage.r2.secret-key}") String secretKey,
            @Value("${carebridge.storage.r2.region:auto}") String region) {
        validate(endpoint, accessKey, secretKey);
        String cleanEndpoint = sanitizeEndpoint(endpoint);
        return S3Client.builder()
                .endpointOverride(URI.create(cleanEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    S3Presigner r2S3Presigner(
            @Value("${carebridge.storage.r2.endpoint}") String endpoint,
            @Value("${carebridge.storage.r2.access-key}") String accessKey,
            @Value("${carebridge.storage.r2.secret-key}") String secretKey,
            @Value("${carebridge.storage.r2.region:auto}") String region) {
        validate(endpoint, accessKey, secretKey);
        String cleanEndpoint = sanitizeEndpoint(endpoint);
        return S3Presigner.builder()
                .endpointOverride(URI.create(cleanEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static String sanitizeEndpoint(String endpoint) {
        if (endpoint == null) return null;
        String trimmed = endpoint.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        int r2Idx = trimmed.indexOf(".r2.cloudflarestorage.com");
        if (r2Idx > 0) {
            int slashAfter = trimmed.indexOf('/', r2Idx);
            if (slashAfter > 0) {
                trimmed = trimmed.substring(0, slashAfter);
            }
        }
        return trimmed;
    }

    private static void validate(String endpoint, String accessKey, String secretKey) {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || accessKey.isBlank()
                || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("R2 storage is selected but its backend credentials are incomplete");
        }
    }
}
