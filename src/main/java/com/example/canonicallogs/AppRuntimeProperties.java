package com.example.canonicallogs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppRuntimeProperties(
    String env,
    String region,
    String version
) {}
