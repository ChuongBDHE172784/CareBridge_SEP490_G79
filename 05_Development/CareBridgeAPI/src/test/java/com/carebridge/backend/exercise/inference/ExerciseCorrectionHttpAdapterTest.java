package com.carebridge.backend.exercise.inference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceRequest;
import com.carebridge.backend.exercise.inference.PostureInferencePort.InferenceResult;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExerciseCorrectionHttpAdapterTest {

    private static final String MODEL_VERSION =
            PostureInferenceConfigResolver.PINNED_MODEL_VERSION;

    @Test
    void infer_sendsLandmarkOnlyVersionedContractAndMapsResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.schemaVersion")
                        .value(ExerciseCorrectionHttpAdapter.LANDMARK_SCHEMA_VERSION))
                .andExpect(jsonPath("$.modelVersion").value(MODEL_VERSION))
                .andExpect(jsonPath("$.exerciseKey").value("squat"))
                .andExpect(jsonPath("$.sequenceNumber").value(42))
                .andExpect(jsonPath("$.inferenceStreamId").doesNotExist())
                .andExpect(jsonPath("$.landmarks.left_shoulder.x").value(0.42))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        InferenceResult result = adapter.infer(validRequest());

        assertThat(result.modelVersion()).isEqualTo(MODEL_VERSION);
        assertThat(result.exerciseKey()).isEqualTo("squat");
        assertThat(result.sequenceNumber()).isEqualTo(42L);
        assertThat(result.predictedClass()).isEqualTo("GOOD_FORM");
        assertThat(result.confidence()).isEqualByComparingTo("0.91");
        assertThat(result.correct()).isTrue();
        assertThat(result.score()).isEqualByComparingTo("88");
        assertThat(result.feedback()).singleElement().satisfies(feedback -> {
            assertThat(feedback.code()).isEqualTo("GOOD_FORM");
            assertThat(feedback.severity()).isEqualTo("INFO");
        });
        server.verify();
    }

    @Test
    void infer_mapsServerErrorToSanitizedUnavailableCode() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"must-not-leak\"}"));

        assertThatThrownBy(() -> adapter.infer(validRequest()))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("SIDECAR_UNAVAILABLE");
        server.verify();
    }

    @Test
    void infer_rejectsMismatchedResponseSequence() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withSuccess(successResponse().replace(
                        "\"sequenceNumber\":42", "\"sequenceNumber\":41"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.infer(validRequest()))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("SIDECAR_INVALID_RESPONSE");
        server.verify();
    }

    @Test
    void infer_whenDisabledDoesNotAttemptHttp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(false, builder.build());

        assertThatThrownBy(() -> adapter.infer(validRequest()))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("PROVIDER_DISABLED");
        server.verify();
    }

    @Test
    void infer_rejectsNonLandmarkFieldsBeforeHttp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());
        InferenceRequest request = new InferenceRequest(
                MODEL_VERSION, "squat", 42L, Map.of("userId", "must-not-leave-backend"));

        assertThatThrownBy(() -> adapter.infer(request))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("INFERENCE_REQUEST_INVALID");
        server.verify();
    }

    @Test
    void infer_mapsNullFeedbackElementToExplicitUnavailable() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withSuccess(
                        successResponse().replace(
                                "{\"code\":\"GOOD_FORM\",\"severity\":\"INFO\",\"message\":\"Keep this form.\"}",
                                "null"),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.infer(validRequest()))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("SIDECAR_INVALID_RESPONSE");
        server.verify();
    }

    @Test
    void infer_carriesTheMovementPhaseSeparatelyFromTheClass() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        // A lunge in the down phase: predictedClass is the error verdict, not the phase.
        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withSuccess(
                        successResponse().replace(
                                "\"predictedClass\":\"GOOD_FORM\"",
                                "\"predictedClass\":\"L\",\"stage\":\"D\""),
                        MediaType.APPLICATION_JSON));

        InferenceResult result = adapter.infer(validRequest());

        assertThat(result.predictedClass()).isEqualTo("L");
        assertThat(result.stage()).isEqualTo("D");
        server.verify();
    }

    @Test
    void infer_acceptsAnOlderSidecarThatOmitsTheStage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        assertThat(adapter.infer(validRequest()).stage()).isNull();
        server.verify();
    }

    @Test
    void infer_rejectsAStageThatIsNotAShortToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withSuccess(
                        successResponse().replace(
                                "\"predictedClass\":\"GOOD_FORM\"",
                                "\"predictedClass\":\"GOOD_FORM\",\"stage\":\"<script>alert(1)</script>\""),
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.infer(validRequest()))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("SIDECAR_INVALID_RESPONSE");
        server.verify();
    }

    @Test
    void infer_mapsClientErrorToRejectedRequestCode() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://exercise-correction:8002");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ExerciseCorrectionHttpAdapter adapter =
                new ExerciseCorrectionHttpAdapter(true, builder.build());

        server.expect(requestTo("http://exercise-correction:8002/v1/inference/landmarks"))
                .andRespond(withStatus(HttpStatusCode.valueOf(422))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"INVALID_INPUT\",\"message\":\"The inference request is invalid\"}"));

        assertThatThrownBy(() -> adapter.infer(validRequest()))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("SIDECAR_REJECTED_REQUEST");
        server.verify();
    }

    @Test
    void sidecarErrorCodeKeepsStableCodesAndSuppressesEverythingElse() {
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode(
                        "{\"code\":\"INVALID_INPUT\",\"message\":\"ignored\"}"))
                .isEqualTo("INVALID_INPUT");
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode(null)).isEqualTo("ABSENT");
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode("   ")).isEqualTo("ABSENT");
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode("<html>502</html>"))
                .isEqualTo("UNPARSEABLE");
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode("{\"detail\":\"must-not-leak\"}"))
                .isEqualTo("UNRECOGNIZED");
        // Free-form text in `code` must never reach the log verbatim.
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode(
                        "{\"code\":\"patient 0123456789 must-not-leak\"}"))
                .isEqualTo("UNRECOGNIZED");
        assertThat(ExerciseCorrectionHttpAdapter.sidecarErrorCode("{\"code\":42}"))
                .isEqualTo("UNRECOGNIZED");
    }

    /**
     * Regression guard: the JDK HttpClient defaults to HTTP_2, which over cleartext
     * negotiates via `Upgrade: h2c`. The sidecar runs uvicorn/h11 (HTTP/1.1 only) and
     * h11 withholds the request body once it sees that header, so every inference came
     * back 422. The adapter must therefore speak plain HTTP/1.1 with no upgrade offer.
     */
    @Test
    void infer_usesPlainHttp11WithoutProtocolUpgrade() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> upgradeHeaders = new CopyOnWriteArrayList<>();
        AtomicReference<String> receivedBody = new AtomicReference<>("");
        server.createContext("/v1/inference/landmarks", exchange -> {
            String upgrade = exchange.getRequestHeaders().getFirst("Upgrade");
            if (upgrade != null) {
                upgradeHeaders.add(upgrade);
            }
            receivedBody.set(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] payload = successResponse().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
        });
        server.start();
        try {
            ExerciseCorrectionHttpAdapter adapter = new ExerciseCorrectionHttpAdapter(
                    true, "http://127.0.0.1:" + server.getAddress().getPort(), 500, 5_000);

            InferenceResult result = adapter.infer(validRequest());

            assertThat(upgradeHeaders).isEmpty();
            assertThat(receivedBody.get()).contains("\"exerciseKey\":\"squat\"");
            assertThat(result.predictedClass()).isEqualTo("GOOD_FORM");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void constructorRejectsUnboundedOrNonPositiveTimeouts() {
        assertThatThrownBy(() -> new ExerciseCorrectionHttpAdapter(
                        true, "http://localhost:8002", 0, 1200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeouts must");
        assertThatThrownBy(() -> new ExerciseCorrectionHttpAdapter(
                        true, "http://localhost:8002", 500, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeouts must");
        assertThatThrownBy(() -> new ExerciseCorrectionHttpAdapter(
                        true, "http://localhost:8002", 10_001, 1200))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeouts must");
        assertThatThrownBy(() -> new ExerciseCorrectionHttpAdapter(
                        true, "http://localhost:8002", 500, 30_001))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeouts must");
    }

    private InferenceRequest validRequest() {
        return new InferenceRequest(
                MODEL_VERSION,
                "squat",
                42L,
                Map.of(
                        "left_shoulder",
                        Map.of(
                                "x", new BigDecimal("0.42"),
                                "y", new BigDecimal("0.31"),
                                "z", new BigDecimal("-0.08"),
                                "visibility", new BigDecimal("0.98"))));
    }

    private String successResponse() {
        return """
                {
                  "schemaVersion":"exercise-correction-inference-v1",
                  "modelVersion":"%s",
                  "exerciseKey":"squat",
                  "sequenceNumber":42,
                  "predictedClass":"GOOD_FORM",
                  "confidence":0.91,
                  "correct":true,
                  "score":88,
                  "feedback":[
                    {"code":"GOOD_FORM","severity":"INFO","message":"Keep this form."}
                  ]
                }
                """.formatted(MODEL_VERSION);
    }
}
