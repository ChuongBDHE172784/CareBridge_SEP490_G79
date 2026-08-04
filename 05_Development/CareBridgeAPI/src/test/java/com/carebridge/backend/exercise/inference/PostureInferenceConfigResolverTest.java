package com.carebridge.backend.exercise.inference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PostureInferenceConfigResolverTest {

    private final PostureInferenceConfigResolver resolver =
            new PostureInferenceConfigResolver(new ObjectMapper());

    @Test
    void resolve_readsPinnedModelAndSupportedExerciseFromServerConfig() {
        PostureAnalysisConfig config = config(
                PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                "{\"exerciseKey\":\"plank\"}");

        PostureInferenceConfigResolver.ResolvedInferenceConfig result = resolver.resolve(config);

        assertThat(result.modelVersion())
                .isEqualTo(PostureInferenceConfigResolver.PINNED_MODEL_VERSION);
        assertThat(result.exerciseKey()).isEqualTo("plank");
    }

    @Test
    void resolve_rejectsUnpinnedModelBeforeCallingProvider() {
        PostureAnalysisConfig config = config(
                "exercise-correction@unreviewed",
                "{\"exerciseKey\":\"plank\"}");

        assertThatThrownBy(() -> resolver.resolve(config))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("CONFIGURATION_INVALID");
    }

    @Test
    void resolve_rejectsUnknownExerciseAndMalformedJson() {
        assertThatThrownBy(() -> resolver.resolve(config(
                        PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                        "{\"exerciseKey\":\"uploaded-model-path\"}")))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("CONFIGURATION_INVALID");

        assertThatThrownBy(() -> resolver.resolve(config(
                        PostureInferenceConfigResolver.PINNED_MODEL_VERSION,
                        "not-json")))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("CONFIGURATION_INVALID");
    }

    @Test
    void resolve_rejectsMissingConfidenceThreshold() {
        PostureAnalysisConfig config = PostureAnalysisConfig.builder()
                .ruleOrModelVersion(PostureInferenceConfigResolver.PINNED_MODEL_VERSION)
                .configJson("{\"exerciseKey\":\"plank\"}")
                .build();

        assertThatThrownBy(() -> resolver.resolve(config))
                .isInstanceOf(PostureInferenceUnavailableException.class)
                .extracting("reasonCode")
                .isEqualTo("CONFIGURATION_INVALID");
    }

    private PostureAnalysisConfig config(String modelVersion, String configJson) {
        return PostureAnalysisConfig.builder()
                .ruleOrModelVersion(modelVersion)
                .confidenceThreshold(new BigDecimal("0.75"))
                .configJson(configJson)
                .build();
    }
}
