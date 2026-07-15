package com.example.contractagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public record StorageProperties(String basePath, long maxFileSizeBytes) {}
