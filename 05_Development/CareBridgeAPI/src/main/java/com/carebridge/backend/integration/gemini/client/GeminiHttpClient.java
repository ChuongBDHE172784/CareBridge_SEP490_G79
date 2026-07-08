package com.carebridge.backend.integration.gemini.client;

import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class GeminiHttpClient implements GeminiClient {



    public GeminiHttpClient(
            @Value("${carebridge.gemini.api-key:}") String apiKey,
            @Value("${carebridge.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${carebridge.gemini.model:gemini-1.5-flash}") String model) {

    }

    @Override
    public String generate(String prompt) throws GeminiUnavailableException {
        throw new GeminiUnavailableException("Gemini HTTP client not configured for this environment");
    }
}
