package com.carebridge.backend.aimoderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.integration.gemini.client.GeminiHttpClient;
import com.carebridge.backend.integration.gemini.exception.GeminiUnavailableException;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The rewritten (real) GeminiHttpClient keeps the legacy contract for its three existing
 * consumers: every failure mode — including disabled — surfaces as GeminiUnavailableException.
 */
class GeminiHttpClientRealTest {

    private static final String API_KEY = "another-secret-key-xyz";

    private static HttpServer server;
    private static int port;
    private static final AtomicInteger hits = new AtomicInteger();
    private static volatile int responseStatus = 200;
    private static volatile String responseBody = "{}";

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            hits.incrementAndGet();
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
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
    }

    private GeminiHttpClient client(boolean enabled, String apiKey) {
        return new GeminiHttpClient(enabled, apiKey, "http://localhost:" + port,
                "gemini-1.5-flash", 500, 500);
    }

    // Scenario 1: disabled → GeminiUnavailableException without any HTTP call
    @Test
    void disabled_throwsUnavailable_withoutHttpCall() {
        assertThatThrownBy(() -> client(false, API_KEY).generate("hello"))
                .isInstanceOf(GeminiUnavailableException.class)
                .hasMessageContaining("disabled");
        assertThat(hits.get()).isZero();
    }

    // Scenario 2: missing key → clear configuration message, no HTTP call
    @Test
    void missingKey_throwsUnavailable_withoutHttpCall() {
        assertThatThrownBy(() -> client(true, " ").generate("hello"))
                .isInstanceOf(GeminiUnavailableException.class)
                .hasMessageContaining("not configured");
        assertThat(hits.get()).isZero();
    }

    // Scenario 3: success extracts the first candidate text
    @Test
    void success_returnsCandidateText() {
        responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"xin chào\"}]}}]}";
        assertThat(client(true, API_KEY).generate("hello")).isEqualTo("xin chào");
    }

    @Test
    void serverError_throwsUnavailable_withoutKeyInMessage() {
        responseStatus = 500;
        assertThatThrownBy(() -> client(true, API_KEY).generate("hello"))
                .isInstanceOf(GeminiUnavailableException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain(API_KEY));
    }

    @Test
    void emptyCandidates_throwsUnavailable_notSilentEmpty() {
        responseBody = "{\"candidates\":[]}";
        assertThatThrownBy(() -> client(true, API_KEY).generate("hello"))
                .isInstanceOf(GeminiUnavailableException.class);
    }
}
