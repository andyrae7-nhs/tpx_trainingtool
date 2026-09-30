package com.tpximpact.trainingtool.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpximpact.trainingtool.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

/** Minimal client for the Anthropic Messages API (https://docs.claude.com). */
@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    private final AppProperties.Ai config;
    private final RestClient http;
    private final ObjectMapper mapper;

    public AnthropicClient(AppProperties props, ObjectMapper mapper) {
        this.config = props.ai();
        this.mapper = mapper;
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(Math.max(5, config.timeoutSeconds())));
        this.http = RestClient.builder()
                .baseUrl(config.baseUrl())
                .requestFactory(factory)
                .defaultHeader("anthropic-version", "2023-06-01")
                .build();
    }

    public boolean enabled() {
        return config.enabled();
    }

    public String model() {
        return config.model();
    }

    public record Message(String role, String content) {}

    /** Sends a conversation and returns the text reply, or empty if AI is disabled or the call fails. */
    public Optional<String> complete(String system, List<Message> messages, int maxTokens) {
        if (!enabled()) return Optional.empty();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", config.model());
            body.put("max_tokens", maxTokens > 0 ? maxTokens : config.maxTokens());
            body.put("system", system);
            body.put("messages", messages.stream().map(m -> Map.of("role", m.role(), "content", m.content())).toList());
            String raw = http.post()
                    .uri("/v1/messages")
                    .header("x-api-key", config.anthropicApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode json = mapper.readTree(raw);
            StringBuilder out = new StringBuilder();
            for (JsonNode block : json.path("content")) {
                if ("text".equals(block.path("type").asText())) out.append(block.path("text").asText());
            }
            return out.isEmpty() ? Optional.empty() : Optional.of(out.toString());
        } catch (Exception e) {
            log.warn("Anthropic API call failed, falling back to built-in logic: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** Pulls the first JSON object out of a model reply (which may be wrapped in prose or code fences). */
    public Optional<JsonNode> extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return Optional.empty();
        try {
            return Optional.of(mapper.readTree(text.substring(start, end + 1)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
