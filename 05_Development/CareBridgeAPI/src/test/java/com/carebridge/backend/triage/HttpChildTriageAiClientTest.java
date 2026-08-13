package com.carebridge.backend.triage;

import static org.assertj.core.api.Assertions.assertThat;

import com.carebridge.backend.triage.dto.request.RunIntakeRequest;
import com.carebridge.backend.triage.service.impl.HttpChildTriageAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class HttpChildTriageAiClientTest {

    private static HttpServer server;
    private static int port;
    private static final AtomicReference<String> requestBody = new AtomicReference<>();
    private static final AtomicReference<String> internalKey = new AtomicReference<>();

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/triage/child", exchange -> {
            internalKey.set(exchange.getRequestHeaders().getFirst("X-CareBridge-Internal-Key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void triageChild_forwardsCommonSymptomDescriptiveFacts() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        HttpChildTriageAiClient client = new HttpChildTriageAiClient(mapper);
        ReflectionTestUtils.setField(client, "aiTriageServiceUrl", "http://127.0.0.1:" + port);
        ReflectionTestUtils.setField(client, "requestTimeoutMs", 2_000L);
        ReflectionTestUtils.setField(client, "internalApiKey", "test-internal-key");

        client.triageChild(RunIntakeRequest.builder()
                .stage(TriageStage.PREGNANCY)
                .painSeverity("vừa")
                .urinarySymptoms("tiểu buốt")
                .hydrationStatus("khô miệng")
                .build());

        var payload = mapper.readTree(requestBody.get());
        assertThat(internalKey.get()).isEqualTo("test-internal-key");
        assertThat(payload.get("painSeverity").asText()).isEqualTo("vừa");
        assertThat(payload.get("urinarySymptoms").asText()).isEqualTo("tiểu buốt");
        assertThat(payload.get("hydrationStatus").asText()).isEqualTo("khô miệng");
    }
}
