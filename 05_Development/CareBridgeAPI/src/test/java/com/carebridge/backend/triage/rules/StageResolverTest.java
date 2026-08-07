package com.carebridge.backend.triage.rules;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1-T4 — stage resolution and entity–stage validation, mirroring
 * {@code tests/test_stage_resolver.py}.
 */
class StageResolverTest {

    private final StageResolver resolver = new StageResolver();

    private StageResolver.StageResolution resolve(TargetEntity entity, CareStage explicit) {
        return resolver.resolve(entity, explicit, null, null, null, null, null);
    }

    @Test
    @DisplayName("An unresolved entity has no stage")
    void unresolvedEntityHasNoStage() {
        // PREGNANCY for whom? Without a subject a stage is meaningless.
        assertThat(resolve(TargetEntity.UNKNOWN, CareStage.PREGNANCY).stage())
                .isEqualTo(CareStage.UNKNOWN);
    }

    @ParameterizedTest
    @CsvSource({"MOTHER,PREGNANCY", "MOTHER,POSTPARTUM_MOTHER",
            "BABY,INFANT_0_12M", "BABY,TODDLER_12_24M"})
    @DisplayName("A valid explicit stage is accepted")
    void validExplicitStageIsAccepted(TargetEntity entity, CareStage stage) {
        var resolution = resolve(entity, stage);
        assertThat(resolution.stage()).isEqualTo(stage);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE);
    }

    @ParameterizedTest
    @CsvSource({"BABY,PREGNANCY", "BABY,POSTPARTUM_MOTHER", "MOTHER,INFANT_0_12M"})
    @DisplayName("A cross-entity stage is CONFLICTED, not silently corrected")
    void crossEntityStageIsConflicted(TargetEntity entity, CareStage stage) {
        var resolution = resolve(entity, stage);
        assertThat(resolution.stage()).isEqualTo(CareStage.CONFLICTED);
        assertThat(resolution.conflicts()).anyMatch(c -> c.contains("STAGE_NOT_VALID_FOR_ENTITY"));
    }

    @Test
    @DisplayName("Legacy POSTPARTUM resolves for the mother")
    void legacyPostpartumResolvesForMother() {
        assertThat(resolver.resolve(TargetEntity.MOTHER, null, "POSTPARTUM", null, null, null, null)
                .stage()).isEqualTo(CareStage.POSTPARTUM_MOTHER);
    }

    @Test
    @DisplayName("Legacy POSTPARTUM with a baby target is CONFLICTED")
    void legacyPostpartumWithBabyIsConflicted() {
        // The legacy name is ambiguous; guessing would pick the wrong subject's stage.
        var resolution = resolver.resolve(TargetEntity.BABY, null, "POSTPARTUM",
                null, null, null, null);
        assertThat(resolution.stage()).isEqualTo(CareStage.CONFLICTED);
        assertThat(resolution.conflicts())
                .anyMatch(c -> c.contains("LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY"));
    }

    @ParameterizedTest
    @CsvSource({"0,INFANT_0_12M", "11,INFANT_0_12M", "12,TODDLER_12_24M", "23,TODDLER_12_24M"})
    @DisplayName("Baby age derives the stage within the supported range")
    void babyAgeDerivesStage(int months, CareStage expected) {
        var resolution = resolver.resolve(TargetEntity.BABY, null, null, null, months, null, null);
        assertThat(resolution.stage()).isEqualTo(expected);
        assertThat(resolution.source()).isEqualTo(ResolutionSource.STAGE_SPECIFIC_CONTEXT);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 24, 36})
    @DisplayName("Baby age outside the supported range is CONFLICTED")
    void babyAgeOutOfRangeIsConflicted(int months) {
        assertThat(resolver.resolve(TargetEntity.BABY, null, null, null, months, null, null).stage())
                .isEqualTo(CareStage.CONFLICTED);
    }

    @Test
    @DisplayName("Gestational week and postpartum day together are CONFLICTED")
    void bothMaternalMeasurementsAreConflicted() {
        // Both cannot be true at once; do not pick the more convenient one.
        assertThat(resolver.resolve(TargetEntity.MOTHER, null, null, null, null, 20, 5).stage())
                .isEqualTo(CareStage.CONFLICTED);
    }

    @Test
    @DisplayName("A single maternal measurement derives the stage")
    void singleMaternalMeasurementDerivesStage() {
        assertThat(resolver.resolve(TargetEntity.MOTHER, null, null, null, null, 20, null).stage())
                .isEqualTo(CareStage.PREGNANCY);
        assertThat(resolver.resolve(TargetEntity.MOTHER, null, null, null, null, null, 5).stage())
                .isEqualTo(CareStage.POSTPARTUM_MOTHER);
    }

    @Test
    @DisplayName("An explicit stage outranks a journey stage")
    void explicitStageOutranksJourney() {
        assertThat(resolver.resolve(TargetEntity.MOTHER, CareStage.PREGNANCY, null,
                CareStage.POSTPARTUM_MOTHER, null, null, null).stage())
                .isEqualTo(CareStage.PREGNANCY);
    }

    @Test
    @DisplayName("Nothing to go on stays UNKNOWN")
    void nothingToGoOnStaysUnknown() {
        assertThat(resolver.resolve(TargetEntity.MOTHER, null, null, null, null, null, null).stage())
                .isEqualTo(CareStage.UNKNOWN);
    }

    // ----------------------------------------------------------------- context status

    @Test
    @DisplayName("A fully resolved triage context is RESOLVED")
    void fullyResolvedContext() {
        var status = resolver.resolveContextStatus(TargetEntity.MOTHER, CareStage.PREGNANCY,
                IntentType.SYMPTOM_TRIAGE, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.RESOLVED);
        assertThat(status.conflicts()).isEmpty();
    }

    @Test
    @DisplayName("An unknown target is asked about first, and blocks symptom questions")
    void unknownTargetNeedsTargetFirst() {
        var status = resolver.resolveContextStatus(TargetEntity.UNKNOWN, CareStage.UNKNOWN,
                IntentType.SYMPTOM_TRIAGE, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.NEEDS_TARGET_ENTITY);
        assertThat(status.status().blocksSymptomQuestions()).isTrue();
    }

    @Test
    @DisplayName("A conflict outranks a mere gap")
    void conflictOutranksGap() {
        // A contradiction cannot be fixed by asking one more question.
        var status = resolver.resolveContextStatus(TargetEntity.CONFLICTED, CareStage.UNKNOWN,
                IntentType.UNKNOWN, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.CONFLICTED);
        assertThat(status.conflicts()).contains("TARGET_ENTITY_CONFLICTED");
    }

    @Test
    @DisplayName("A cross-entity pair surfaces as a context conflict")
    void crossEntityPairSurfacesAsConflict() {
        var status = resolver.resolveContextStatus(TargetEntity.BABY, CareStage.PREGNANCY,
                IntentType.SYMPTOM_TRIAGE, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.CONFLICTED);
        assertThat(status.conflicts()).anyMatch(c -> c.contains("STAGE_NOT_VALID_FOR_ENTITY"));
    }

    @Test
    @DisplayName("A general question needs no stage")
    void generalQuestionNeedsNoStage() {
        var status = resolver.resolveContextStatus(TargetEntity.MOTHER, CareStage.UNKNOWN,
                IntentType.GENERAL_HEALTH_INFORMATION, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.RESOLVED);
    }

    @Test
    @DisplayName("A triage intent without a stage needs the stage")
    void triageIntentNeedsStage() {
        var status = resolver.resolveContextStatus(TargetEntity.MOTHER, CareStage.UNKNOWN,
                IntentType.SYMPTOM_TRIAGE, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.NEEDS_STAGE);
    }

    @Test
    @DisplayName("An unknown intent is asked about before the stage")
    void unknownIntentComesBeforeStage() {
        var status = resolver.resolveContextStatus(TargetEntity.MOTHER, CareStage.UNKNOWN,
                IntentType.UNKNOWN, null);
        assertThat(status.status()).isEqualTo(ContextResolutionStatus.NEEDS_INTENT);
    }

    @Test
    @DisplayName("validateEntityStage reports only real mismatches")
    void validateReportsOnlyRealMismatches() {
        assertThat(resolver.validateEntityStage(TargetEntity.MOTHER, CareStage.PREGNANCY)).isEmpty();
        assertThat(resolver.validateEntityStage(TargetEntity.UNKNOWN, CareStage.PREGNANCY)).isEmpty();
        assertThat(resolver.validateEntityStage(TargetEntity.BABY, CareStage.PREGNANCY)).isNotEmpty();
    }
}
