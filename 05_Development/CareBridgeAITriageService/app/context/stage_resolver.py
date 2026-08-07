"""Stage resolution and entity–stage validation.

Two jobs, deliberately in one place because they are the same question asked twice:

1. Which lifecycle stage applies?
2. Is that stage even *possible* for the entity we resolved?

A stage that does not belong to the entity is not a rounding error. ``targetEntity=BABY`` with
``stage=PREGNANCY`` means the engine is about to reason about a pregnancy that belongs to
someone who is not the subject of the session. That is CONFLICTED, never silently corrected.
"""

from __future__ import annotations

from dataclasses import dataclass

from app.context.enums import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
    map_legacy_stage,
    stages_for_entity,
)


@dataclass(frozen=True)
class StageResolution:
    stage: CareStage
    source: ResolutionSource
    conflicts: tuple[str, ...] = ()

    @property
    def is_resolved(self) -> bool:
        return self.stage.is_resolved


def resolve_stage(
    *,
    entity: TargetEntity,
    explicit_stage: CareStage | None = None,
    legacy_stage_name: str | None = None,
    journey_stage: CareStage | None = None,
    baby_age_months: int | None = None,
    gestational_week: int | None = None,
    postpartum_day: int | None = None,
) -> StageResolution:
    """Resolve the care stage for an already-resolved entity."""

    if not entity.is_resolved:
        # Without a subject, a stage is meaningless — PREGNANCY for whom?
        return StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE)

    valid = stages_for_entity(entity)

    if explicit_stage is not None and explicit_stage.is_resolved:
        if explicit_stage not in valid:
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                (f"STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{explicit_stage.value}",))
        return StageResolution(explicit_stage, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE)

    if legacy_stage_name:
        mapped = map_legacy_stage(legacy_stage_name, entity)
        if mapped is None:
            # Legacy POSTPARTUM with a BABY target lands here on purpose: the name is
            # ambiguous and guessing would pick the wrong subject's stage.
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.EXPLICIT_SELECTED_PROFILE,
                (f"LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY:{entity.value}/{legacy_stage_name}",))
        return StageResolution(mapped, ResolutionSource.EXPLICIT_SELECTED_PROFILE)

    if journey_stage is not None and journey_stage.is_resolved:
        if journey_stage not in valid:
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.CONFIRMED_CONVERSATION_TARGET,
                (f"JOURNEY_STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{journey_stage.value}",))
        return StageResolution(journey_stage, ResolutionSource.CONFIRMED_CONVERSATION_TARGET)

    # Derive from measurements only as a last resort, and only within the entity's own range.
    if entity is TargetEntity.BABY and baby_age_months is not None:
        if baby_age_months < 0 or baby_age_months >= 24:
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                (f"BABY_AGE_OUT_OF_SUPPORTED_RANGE:{baby_age_months}",))
        derived = CareStage.INFANT_0_12M if baby_age_months < 12 else CareStage.TODDLER_12_24M
        return StageResolution(derived, ResolutionSource.STAGE_SPECIFIC_CONTEXT)

    if entity is TargetEntity.MOTHER:
        if gestational_week is not None and postpartum_day is not None:
            # Both cannot be true at once; do not pick the more convenient one.
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                ("GESTATIONAL_WEEK_AND_POSTPARTUM_DAY_BOTH_PRESENT",))
        if gestational_week is not None:
            return StageResolution(CareStage.PREGNANCY, ResolutionSource.STAGE_SPECIFIC_CONTEXT)
        if postpartum_day is not None:
            return StageResolution(CareStage.POSTPARTUM_MOTHER,
                                   ResolutionSource.STAGE_SPECIFIC_CONTEXT)

    return StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE)


def validate_entity_stage(entity: TargetEntity, stage: CareStage) -> tuple[str, ...]:
    """Return conflict codes for an entity–stage pair, empty when the pair is coherent."""

    if not entity.is_resolved or not stage.is_resolved:
        return ()
    if stage in stages_for_entity(entity):
        return ()
    return (f"STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{stage.value}",)


def resolve_context_status(
    *,
    entity: TargetEntity,
    stage: CareStage,
    intent: IntentType,
    extra_conflicts: tuple[str, ...] = (),
) -> tuple[ContextResolutionStatus, tuple[str, ...]]:
    """Combine the three resolutions into one status, plus the conflicts behind it.

    Order matters: a conflict is reported before a mere gap, because a contradiction cannot be
    fixed by asking one more question — it needs the user to choose.
    """

    conflicts = tuple(extra_conflicts) + validate_entity_stage(entity, stage)

    if entity is TargetEntity.CONFLICTED:
        conflicts += ("TARGET_ENTITY_CONFLICTED",)
    if stage is CareStage.CONFLICTED:
        conflicts += ("CARE_STAGE_CONFLICTED",)
    if intent is IntentType.CONFLICTED:
        conflicts += ("INTENT_CONFLICTED",)

    if conflicts:
        return ContextResolutionStatus.CONFLICTED, conflicts
    if not entity.is_resolved:
        return ContextResolutionStatus.NEEDS_TARGET_ENTITY, conflicts
    if not intent.is_resolved:
        return ContextResolutionStatus.NEEDS_INTENT, conflicts
    if not intent.may_produce_triage_outcome:
        # A general or source question needs no stage; it is fully resolved as it stands.
        return ContextResolutionStatus.RESOLVED, conflicts
    if not stage.is_resolved:
        return ContextResolutionStatus.NEEDS_STAGE, conflicts
    return ContextResolutionStatus.RESOLVED, conflicts
