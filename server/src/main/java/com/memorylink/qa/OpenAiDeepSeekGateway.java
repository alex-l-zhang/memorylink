package com.memorylink.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.memorylink.common.BusinessException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiDeepSeekGateway implements DeepSeekGateway {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiDeepSeekGateway(@Value("${memorylink.deepseek.base-url}") String baseUrl,
                                 @Value("${memorylink.deepseek.api-key}") String apiKey,
                                 @Value("${memorylink.deepseek.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userQuestion) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(5010, "AI 服务未配置，请联系管理员");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.7,
                "max_tokens", 500,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userQuestion)
                )
        );
        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("choices") || response.get("choices").isEmpty()) {
                throw new BusinessException(5010, "AI 服务暂时不可用，请稍后重试");
            }
            String content = response.get("choices").get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new BusinessException(5010, "AI 服务暂时不可用，请稍后重试");
            }
            return content.trim();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(5010, "AI 服务暂时不可用，请稍后重试");
        }
    }
}
