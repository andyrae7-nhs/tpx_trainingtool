package com.tpximpact.trainingtool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, boolean seedDemoData, Ai ai) {

    public record Jwt(String secret, int expiryHours) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Ai(String anthropicApiKey, String model, String baseUrl, int maxTokens, int timeoutSeconds) {
        public boolean enabled() {
            return anthropicApiKey != null && !anthropicApiKey.isBlank();
        }
    }
}
