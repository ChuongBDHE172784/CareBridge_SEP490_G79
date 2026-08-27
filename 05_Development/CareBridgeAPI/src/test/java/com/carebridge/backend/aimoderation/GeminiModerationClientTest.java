package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.integration.gemini.client.GeminiModerationClient;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient.ConfigState;
import com.carebridge.backend.integration.gemini.client.GeminiModerationClient.ModerationCallResult;
import com.carebridge.backend.integration.gemini.exception.GeminiConfigurationException;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * HTTP-level tests against a local JDK HttpServer (repo has no WireMock/MockRestServiceServer;
 * this adds no dependency and never touches the real Gemini API).
 */
class GeminiModerationClientTest {

    private static final String API_KEY = "test-secret-api-key-abc123";

    private static HttpServer server;
    private static int port;
    private static final AtomicInteger hits = new AtomicInteger();
    private static volatile int responseStatus = 200;
    private static volatile String responseBody = "{}";
    private static volatile long delayMs = 0;

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            try {
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @BeforeEach
    void reset() {
        hits.set(0);
        responseStatus = 200;
        responseBody = "{}";
        delayMs = 0;
    }

    private GeminiModerationClient client(boolean enabled, String apiKey) {
        return new GeminiModerationClient(enabled, apiKey, "http://localhost:" + port,
                "gemini-1.5-flash", 500, 500);
    }

    private static Map<String, Object> schema() {
        return Map.of("type", "OBJECT");
    }

    // Scenario 1: disabled — clear state, no HTTP request at all
    @Test
    void disabled_reportsDisabledState_andNeverCallsHttp() {
        GeminiModerationClient client = client(false, API_KEY);
        assertThat(client.configState()).isEqualTo(ConfigState.DISABLED);
        assertThatThrownBy(() -> client.classify("sys", "content", schema()))
                .isInstanceOf(GeminiConfigurationException.class)
                .extracting(ex -> ((GeminiConfigurationException) ex).getErrorCode())
                .isEqualTo("GEMINI_DISABLED");
        assertThat(hits.get()).isZero();
    }

    // Scenario 2: missing key — explicit configuration state, no HTTP
    @Test
    void missingKey_reportsNotConfigured_andNeverCallsHttp() {
        GeminiModerationClient client = client(true, "");
        assertThat(client.configState()).isEqualTo(ConfigState.NOT_CONFIGURED);
        assertThatThrownBy(() -> client.classify("sys", "content", schema()))
                .isInstanceOf(GeminiConfigurationException.class)
                .extracting(ex -> ((GeminiConfigurationException) ex).getErrorCode())
                .isEqualTo("GEMINI_KEY_MISSING");
        assertThat(hits.get()).isZero();
    }

    // Scenario 3: success parses structured response + usage metadata
    @Test
    void success_returnsRawJsonAndUsage() {
        responseBody = """
                {"candidates":[{"content":{"parts":[{"text":"{\\"classification\\":\\"SAFE\\"}"}]}}],
                 "usageMetadata":{"promptTokenCount":42,"candidatesTokenCount":7}}
                """;
        ModerationCallResult result = client(true, API_KEY).classify("sys", "content", schema());
        assertThat(result.rawJson()).contains("SAFE");
        assertThat(result.promptTokens()).isEqualTo(42);
        assertThat(result.outputTokens()).isEqualTo(7);
        assertThat(hits.get()).isEqualTo(1);
    }

    // Scenario 5: 429 and 5xx are retryable
    @Test
    void http429_isRetryableUnavailable() {
        responseStatus = 429;
        assertThatThrownBy(() -> client(true, API_KEY).classify("sys", "content", schema()))
                .isInstanceOf(GeminiUnavailableException.class);
    }

    @Test
    void http503_isRetryableUnavailable() {
        responseStatus = 503;
        assertThatThrownBy(() -> client(true, API_KEY).classify("sys", "content", schema()))
                .isInstanceOf(GeminiUnavailableException.class);
    }

    @Test
    void timeout_isRetryableUnavailable() {
        delayMs = 2000; // read timeout is 500ms
        assertThatThrownBy(() -> client(true, API_KEY).classify("sys", "content", schema()))
                .isInstanceOf(GeminiUnavailableException.class);
    }

    // Scenario 6: 4xx configuration/model errors are terminal, not endlessly retried
    @Test
    void http404_isModelConfigurationError() {
        responseStatus = 404;
        assertThatThrownBy(() -> client(true, API_KEY).classify("sys", "content", schema()))
                .isInstanceOf(GeminiConfigurationException.class)
                .extracting(ex -> ((GeminiConfigurationException) ex).getErrorCode())
                .isEqualTo("GEMINI_MODEL_INVALID");
    }

    @Test
    void http401_isAuthConfigurationError() {
        responseStatus = 401;
        assertThatThrownBy(() -> client(true, API_KEY).classify("sys", "content", schema()))
                .isInstanceOf(GeminiConfigurationException.class)
                .extracting(ex -> ((GeminiConfigurationException) ex).getErrorCode())
                .isEqualTo("GEMINI_AUTH_FAILED");
    }

    // Provider safety block is surfaced as terminal failure — never SAFE
    @Test
    void blockReason_isSafetyBlockedFailure() {
        responseBody = "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}";
        assertThatThrownBy(() -> client(true, API_KEY).classify("sys", "content", schema()))
                .isInstanceOf(GeminiConfigurationException.class)
                .extracting(ex -> ((GeminiConfigurationException) ex).getErrorCode())
                .isEqualTo("GEMINI_SAFETY_BLOCKED");
    }

    // Scenario 7: the API key never leaks into any error message
    @Test
    void errors_neverContainApiKey() {
        for (int status : new int[] {400, 401, 403, 404, 429, 500, 503}) {
            responseStatus = status;
            try {
                client(true, API_KEY).classify("sys", "content", schema());
            } catch (RuntimeException ex) {
                assertThat(String.valueOf(ex.getMessage())).doesNotContain(API_KEY);
            }
        }
    }
}
