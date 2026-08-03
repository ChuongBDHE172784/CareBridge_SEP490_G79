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
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
