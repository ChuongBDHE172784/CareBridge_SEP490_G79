package com.carebridge.backend.exercise.policy;

import com.carebridge.backend.exercise.entity.AnalysisMode;
import com.carebridge.backend.exercise.entity.PostureAnalysisConfig;
import com.carebridge.backend.exercise.entity.PregnancyExercise;
import com.carebridge.backend.exercise.exception.InvalidExerciseStateException;
import com.carebridge.backend.exercise.inference.PostureInferenceConfigResolver;
import com.carebridge.backend.exercise.inference.PostureInferenceUnavailableException;
import com.carebridge.backend.exercise.repository.PostureAnalysisConfigRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Component;

/** Static publication checks for exercises that advertise posture analysis. */
@Component
public class ExercisePublishReadinessPolicy {

    private final PostureAnalysisConfigRepository postureConfigRepository;
    private final PostureInferenceConfigResolver inferenceConfigResolver;
    private final Clock clock;

    @Autowired
    public ExercisePublishReadinessPolicy(
            PostureAnalysisConfigRepository postureConfigRepository,
            PostureInferenceConfigResolver inferenceConfigResolver) {
        this(postureConfigRepository, inferenceConfigResolver, Clock.systemUTC());
    }

    public ExercisePublishReadinessPolicy(
            PostureAnalysisConfigRepository postureConfigRepository,
            PostureInferenceConfigResolver inferenceConfigResolver,
            Clock clock) {
        this.postureConfigRepository = postureConfigRepository;
        this.inferenceConfigResolver = inferenceConfigResolver;
        this.clock = clock;
    }

    public void verifyReady(PregnancyExercise exercise) {
        if (!Boolean.TRUE.equals(exercise.getSupportsPostureAnalysis())) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        PostureAnalysisConfig config;
        try {
            config = postureConfigRepository
                    .findActiveConfigByExerciseId(exercise.getExerciseId(), now)
                    .orElseThrow(InvalidExerciseStateException::postureNotReady);
        } catch (IncorrectResultSizeDataAccessException exception) {
            // Duplicate effective ACTIVE rows are an invalid static configuration, not a 500.
            throw InvalidExerciseStateException.postureNotReady();
        }

        AnalysisMode mode = validatedMode(config, now);
        if (mode == AnalysisMode.MODEL_BASED || mode == AnalysisMode.HYBRID) {
            try {
                inferenceConfigResolver.resolve(config);
            } catch (PostureInferenceUnavailableException exception) {
                throw InvalidExerciseStateException.postureNotReady();
            }
        }
    }

    private AnalysisMode validatedMode(PostureAnalysisConfig config, OffsetDateTime now) {
        if (!"ACTIVE".equals(config.getStatus())
                || config.getEffectiveFrom() == null
                || config.getEffectiveFrom().isAfter(now)
                || (config.getEffectiveTo() != null && !config.getEffectiveTo().isAfter(now))
                || !validThreshold(config.getConfidenceThreshold())) {
            throw InvalidExerciseStateException.postureNotReady();
        }

        try {
            return AnalysisMode.valueOf(config.getAnalysisMode());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw InvalidExerciseStateException.postureNotReady();
        }
    }

    private boolean validThreshold(BigDecimal threshold) {
        return threshold != null
                && threshold.compareTo(BigDecimal.ZERO) >= 0
                && threshold.compareTo(BigDecimal.ONE) <= 0;
    }
}
