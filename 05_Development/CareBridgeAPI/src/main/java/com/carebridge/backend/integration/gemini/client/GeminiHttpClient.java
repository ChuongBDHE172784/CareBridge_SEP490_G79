package com.carebridge.backend.integration.gemini.client;

import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Profile({"prod", "dev", "supabase"})
@Slf4j
public class GeminiHttpClient implements GeminiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public GeminiHttpClient(
            @Value("${carebridge.gemini.api-key:}") String apiKey,
            @Value("${carebridge.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${carebridge.gemini.model:gemini-1.5-flash}") String model) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    @Override
    public String generate(String prompt) throws GeminiUnavailableException {
        throw new GeminiUnavailableException("Gemini HTTP client not configured for this environment");
    }
}
