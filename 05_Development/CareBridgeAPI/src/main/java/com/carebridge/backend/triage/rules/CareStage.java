package com.carebridge.backend.triage.rules;

import java.util.List;
import java.util.Map;

/**
 * Lifecycle stage, split by entity so a maternal threshold can never be applied to an infant.
 * Kept in lockstep with {@code Contracts/triage/context_contract_v1.json}.
 */
public enum CareStage {
    PRECONCEPTION,
    POSSIBLE_PREGNANCY,
    PREGNANCY,
    POSTPARTUM_MOTHER,
    INFANT_0_12M,
    TODDLER_12_24M,
    UNKNOWN,
    CONFLICTED;

    private static final Map<TargetEntity, List<CareStage>> BY_ENTITY = Map.of(
            TargetEntity.MOTHER,
            List.of(PRECONCEPTION, POSSIBLE_PREGNANCY, PREGNANCY, POSTPARTUM_MOTHER),
            TargetEntity.BABY,
            List.of(INFANT_0_12M, TODDLER_12_24M));

    public boolean isResolved() {
        return this != UNKNOWN && this != CONFLICTED;
    }

    /** Stages valid for an entity. An unresolved entity has none — that is deliberate. */
    public static List<CareStage> forEntity(TargetEntity entity) {
        return BY_ENTITY.getOrDefault(entity, List.of());
    }

    public static boolean isValidFor(TargetEntity entity, CareStage stage) {
        return forEntity(entity).contains(stage);
    }

    /**
     * Maps a legacy stage name for a given target, or {@code null} when it must not be mapped.
     *
     * <p>Legacy {@code POSTPARTUM} is why this exists: it named the maternal stage, but a
     * postpartum session may equally be about the newborn. It maps only for MOTHER; with BABY
     * or an unresolved target the caller must resolve the target first rather than guess.
     */
    public static CareStage mapLegacy(String legacy, TargetEntity entity) {
        if (legacy == null) {
            return null;
        }
        return switch (legacy) {
            case "POSTPARTUM" -> entity == TargetEntity.MOTHER ? POSTPARTUM_MOTHER : null;
            case "PRECONCEPTION" -> entity == TargetEntity.BABY || entity == TargetEntity.CONFLICTED
                    ? null : PRECONCEPTION;
            case "POSSIBLE_PREGNANCY" -> entity == TargetEntity.BABY || entity == TargetEntity.CONFLICTED
                    ? null : POSSIBLE_PREGNANCY;
            case "PREGNANCY" -> entity == TargetEntity.BABY || entity == TargetEntity.CONFLICTED
                    ? null : PREGNANCY;
            case "INFANT" -> entity == TargetEntity.BABY ? INFANT_0_12M : null;
            case "TODDLER" -> entity == TargetEntity.BABY ? TODDLER_12_24M : null;
            default -> null;
        };
    }
}
