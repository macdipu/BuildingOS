package com.buildingos.building.document.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code buildingos.documents.*}. Empty {@code endpoint} = AWS S3; empty keys = the default AWS credential chain
 * (instance/task role in production).
 */
@ConfigurationProperties("buildingos.documents")
public record DocumentProperties(long maxSizeBytes, int maxPerApplication, S3 s3) {
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey,
            boolean pathStyle) {}
}
