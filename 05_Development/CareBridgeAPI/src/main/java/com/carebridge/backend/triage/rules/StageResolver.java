package com.carebridge.backend.triage.rules;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage resolution and entity–stage validation.
 *
 * <p>Two jobs in one place because they are the same question asked twice: which stage
 * applies, and is that stage even possible for the entity we resolved?
 *
 * <p>A stage that does not belong to the entity is not a rounding error.
 * {@code targetEntity=BABY} with {@code stage=PREGNANCY} means the engine is about to reason
 * about a pregnancy belonging to someone who is not the subject of the session. That is
 * CONFLICTED, never silently corrected.
 *
 * <p>Behavioural parity with {@code app/context/stage_resolver.py}.
 */
@Component
public class StageResolver {

    public record StageResolution(CareStage stage, ResolutionSource source, List<String> conflicts) {
        public boolean isResolved() {
            return stage.isResolved();
        }
    }

    public record ContextStatus(ContextResolutionStatus status, List<String> conflicts) {
    }

    public StageResolution resolve(
            TargetEntity entity,
            CareStage explicitStage,
            String legacyStageName,
            CareStage journeyStage,
            Integer babyAgeMonths,
            Integer gestationalWeek,
            Integer postpartumDay) {

        if (!entity.isResolved()) {
            // Without a subject a stage is meaningless — PREGNANCY for whom?
            return new StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE, List.of());
        }

        if (explicitStage != null && explicitStage.isResolved()) {
            if (!CareStage.isValidFor(entity, explicitStage)) {
                return conflicted(ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                        "STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + explicitStage);
            }
            return new StageResolution(explicitStage, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                    List.of());
        }

        if (legacyStageName != null && !legacyStageName.isBlank()) {
            CareStage mapped = CareStage.mapLegacy(legacyStageName, entity);
            if (mapped == null) {
                // Legacy POSTPARTUM with a BABY target lands here on purpose: the name is
                // ambiguous and guessing would pick the wrong subject's stage.
                return conflicted(ResolutionSource.EXPLICIT_SELECTED_PROFILE,
                        "LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY:" + entity + "/" + legacyStageName);
            }
            return new StageResolution(mapped, ResolutionSource.EXPLICIT_SELECTED_PROFILE, List.of());
        }

        if (journeyStage != null && journeyStage.isResolved()) {
            if (!CareStage.isValidFor(entity, journeyStage)) {
                return conflicted(ResolutionSource.CONFIRMED_CONVERSATION_TARGET,
                        "JOURNEY_STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + journeyStage);
            }
            return new StageResolution(journeyStage, ResolutionSource.CONFIRMED_CONVERSATION_TARGET,
                    List.of());
        }

        // Derive from measurements only as a last resort, within the entity's own range.
        if (entity == TargetEntity.BABY && babyAgeMonths != null) {
            if (babyAgeMonths < 0 || babyAgeMonths >= 24) {
                return conflicted(ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                        "BABY_AGE_OUT_OF_SUPPORTED_RANGE:" + babyAgeMonths);
            }
            CareStage derived = babyAgeMonths < 12 ? CareStage.INFANT_0_12M : CareStage.TODDLER_12_24M;
            return new StageResolution(derived, ResolutionSource.STAGE_SPECIFIC_CONTEXT, List.of());
        }

        if (entity == TargetEntity.MOTHER) {
            if (gestationalWeek != null && postpartumDay != null) {
                // Both cannot be true at once; do not pick the more convenient one.
                return conflicted(ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                        "GESTATIONAL_WEEK_AND_POSTPARTUM_DAY_BOTH_PRESENT");
            }
            if (gestationalWeek != null) {
                return new StageResolution(CareStage.PREGNANCY,
                        ResolutionSource.STAGE_SPECIFIC_CONTEXT, List.of());
            }
            if (postpartumDay != null) {
                return new StageResolution(CareStage.POSTPARTUM_MOTHER,
                        ResolutionSource.STAGE_SPECIFIC_CONTEXT, List.of());
            }
        }

        return new StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE, List.of());
    }

    /** Conflict codes for an entity–stage pair; empty when the pair is coherent. */
    public List<String> validateEntityStage(TargetEntity entity, CareStage stage) {
        if (!entity.isResolved() || !stage.isResolved()) {
            return List.of();
        }
        if (CareStage.isValidFor(entity, stage)) {
            return List.of();
        }
        return List.of("STAGE_NOT_VALID_FOR_ENTITY:" + entity + "/" + stage);
    }

    /**
     * Combine the three resolutions into one status. A conflict is reported before a mere gap,
     * because a contradiction cannot be fixed by asking one more question — the user must choose.
     */
    public ContextStatus resolveContextStatus(
            TargetEntity entity, CareStage stage, IntentType intent, List<String> extraConflicts) {

        List<String> conflicts = new ArrayList<>();
        if (extraConflicts != null) {
            conflicts.addAll(extraConflicts);
        }
        conflicts.addAll(validateEntityStage(entity, stage));
        if (entity == TargetEntity.CONFLICTED) conflicts.add("TARGET_ENTITY_CONFLICTED");
        if (stage == CareStage.CONFLICTED) conflicts.add("CARE_STAGE_CONFLICTED");
        if (intent == IntentType.CONFLICTED) conflicts.add("INTENT_CONFLICTED");

        if (!conflicts.isEmpty()) {
            return new ContextStatus(ContextResolutionStatus.CONFLICTED, List.copyOf(conflicts));
        }
        if (!entity.isResolved()) {
            return new ContextStatus(ContextResolutionStatus.NEEDS_TARGET_ENTITY, List.of());
        }
        if (!intent.isResolved()) {
            return new ContextStatus(ContextResolutionStatus.NEEDS_INTENT, List.of());
        }
        if (!intent.mayProduceTriageOutcome()) {
            // A general or source question needs no stage; it is resolved as it stands.
            return new ContextStatus(ContextResolutionStatus.RESOLVED, List.of());
        }
        if (!stage.isResolved()) {
            return new ContextStatus(ContextResolutionStatus.NEEDS_STAGE, List.of());
        }
        return new ContextStatus(ContextResolutionStatus.RESOLVED, List.of());
    }

    private static StageResolution conflicted(ResolutionSource source, String code) {
        return new StageResolution(CareStage.CONFLICTED, source, List.of(code));
    }
}
