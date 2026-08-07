"""P1-T4 — stage resolution and entity–stage validation.

A stage that does not belong to the entity is not a rounding error: ``BABY`` with
``PREGNANCY`` means the engine is about to reason about someone who is not the subject of the
session. These tests pin that such pairs become CONFLICTED, never silently corrected.
"""

from __future__ import annotations

import pytest

from app.context.enums import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
)
from app.context.stage_resolver import (
    resolve_context_status,
    resolve_stage,
    validate_entity_stage,
)


# ------------------------------------------------------------------ stage resolution


def test_an_unresolved_entity_has_no_stage():
    """PREGNANCY for whom? Without a subject a stage is meaningless."""

    resolution = resolve_stage(entity=TargetEntity.UNKNOWN, explicit_stage=CareStage.PREGNANCY)
    assert resolution.stage is CareStage.UNKNOWN


@pytest.mark.parametrize(
    "entity,stage",
    [
        (TargetEntity.MOTHER, CareStage.PREGNANCY),
        (TargetEntity.MOTHER, CareStage.POSTPARTUM_MOTHER),
        (TargetEntity.BABY, CareStage.INFANT_0_12M),
        (TargetEntity.BABY, CareStage.TODDLER_12_24M),
    ],
)
def test_a_valid_explicit_stage_is_accepted(entity, stage):
    resolution = resolve_stage(entity=entity, explicit_stage=stage)
    assert resolution.stage is stage
    assert resolution.source is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE


@pytest.mark.parametrize(
    "entity,stage",
    [
        (TargetEntity.BABY, CareStage.PREGNANCY),
        (TargetEntity.BABY, CareStage.POSTPARTUM_MOTHER),
        (TargetEntity.MOTHER, CareStage.INFANT_0_12M),
    ],
)
def test_a_cross_entity_stage_is_conflicted_not_corrected(entity, stage):
    resolution = resolve_stage(entity=entity, explicit_stage=stage)
    assert resolution.stage is CareStage.CONFLICTED
    assert any("STAGE_NOT_VALID_FOR_ENTITY" in code for code in resolution.conflicts)


def test_legacy_postpartum_resolves_for_the_mother():
    resolution = resolve_stage(entity=TargetEntity.MOTHER, legacy_stage_name="POSTPARTUM")
    assert resolution.stage is CareStage.POSTPARTUM_MOTHER


def test_legacy_postpartum_with_a_baby_target_is_conflicted():
    """The legacy name is ambiguous; guessing would pick the wrong subject's stage."""

    resolution = resolve_stage(entity=TargetEntity.BABY, legacy_stage_name="POSTPARTUM")
    assert resolution.stage is CareStage.CONFLICTED
    assert any("LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY" in code for code in resolution.conflicts)


@pytest.mark.parametrize(
    "months,expected",
    [(0, CareStage.INFANT_0_12M), (11, CareStage.INFANT_0_12M),
     (12, CareStage.TODDLER_12_24M), (23, CareStage.TODDLER_12_24M)],
)
def test_baby_age_derives_the_stage_within_range(months, expected):
    resolution = resolve_stage(entity=TargetEntity.BABY, baby_age_months=months)
    assert resolution.stage is expected
    assert resolution.source is ResolutionSource.STAGE_SPECIFIC_CONTEXT


@pytest.mark.parametrize("months", [-1, 24, 36])
def test_baby_age_outside_the_supported_range_is_conflicted(months):
    resolution = resolve_stage(entity=TargetEntity.BABY, baby_age_months=months)
    assert resolution.stage is CareStage.CONFLICTED


def test_gestational_week_and_postpartum_day_together_are_conflicted():
    """Both cannot be true at once; do not pick the more convenient one."""

    resolution = resolve_stage(
        entity=TargetEntity.MOTHER, gestational_week=20, postpartum_day=5)
    assert resolution.stage is CareStage.CONFLICTED


def test_a_single_maternal_measurement_derives_the_stage():
    assert resolve_stage(entity=TargetEntity.MOTHER, gestational_week=20).stage \
        is CareStage.PREGNANCY
    assert resolve_stage(entity=TargetEntity.MOTHER, postpartum_day=5).stage \
        is CareStage.POSTPARTUM_MOTHER


def test_explicit_stage_outranks_a_journey_stage():
    resolution = resolve_stage(
        entity=TargetEntity.MOTHER,
        explicit_stage=CareStage.PREGNANCY,
        journey_stage=CareStage.POSTPARTUM_MOTHER,
    )
    assert resolution.stage is CareStage.PREGNANCY


def test_nothing_to_go_on_stays_unknown():
    assert resolve_stage(entity=TargetEntity.MOTHER).stage is CareStage.UNKNOWN


# ----------------------------------------------------------------------- validation


def test_validate_entity_stage_reports_only_real_mismatches():
    assert validate_entity_stage(TargetEntity.MOTHER, CareStage.PREGNANCY) == ()
    assert validate_entity_stage(TargetEntity.UNKNOWN, CareStage.PREGNANCY) == ()
    assert validate_entity_stage(TargetEntity.BABY, CareStage.PREGNANCY) != ()


# -------------------------------------------------------------------- context status


def test_a_fully_resolved_triage_context_is_resolved():
    status, conflicts = resolve_context_status(
        entity=TargetEntity.MOTHER, stage=CareStage.PREGNANCY,
        intent=IntentType.SYMPTOM_TRIAGE)
    assert status is ContextResolutionStatus.RESOLVED
    assert conflicts == ()


def test_an_unknown_target_needs_the_target_first():
    status, _ = resolve_context_status(
        entity=TargetEntity.UNKNOWN, stage=CareStage.UNKNOWN,
        intent=IntentType.SYMPTOM_TRIAGE)
    assert status is ContextResolutionStatus.NEEDS_TARGET_ENTITY
    assert status.blocks_symptom_questions is True


def test_a_conflict_outranks_a_mere_gap():
    """A contradiction cannot be fixed by asking one more question."""

    status, conflicts = resolve_context_status(
        entity=TargetEntity.CONFLICTED, stage=CareStage.UNKNOWN, intent=IntentType.UNKNOWN)
    assert status is ContextResolutionStatus.CONFLICTED
    assert "TARGET_ENTITY_CONFLICTED" in conflicts


def test_a_cross_entity_pair_surfaces_as_a_context_conflict():
    status, conflicts = resolve_context_status(
        entity=TargetEntity.BABY, stage=CareStage.PREGNANCY,
        intent=IntentType.SYMPTOM_TRIAGE)
    assert status is ContextResolutionStatus.CONFLICTED
    assert any("STAGE_NOT_VALID_FOR_ENTITY" in code for code in conflicts)


def test_a_general_question_needs_no_stage():
    """Asking what a danger sign is does not require knowing the asker's stage."""

    status, _ = resolve_context_status(
        entity=TargetEntity.MOTHER, stage=CareStage.UNKNOWN,
        intent=IntentType.GENERAL_HEALTH_INFORMATION)
    assert status is ContextResolutionStatus.RESOLVED


def test_a_triage_intent_without_a_stage_needs_the_stage():
    status, _ = resolve_context_status(
        entity=TargetEntity.MOTHER, stage=CareStage.UNKNOWN,
        intent=IntentType.SYMPTOM_TRIAGE)
    assert status is ContextResolutionStatus.NEEDS_STAGE


def test_an_unknown_intent_is_asked_about_before_the_stage():
    status, _ = resolve_context_status(
        entity=TargetEntity.MOTHER, stage=CareStage.UNKNOWN, intent=IntentType.UNKNOWN)
    assert status is ContextResolutionStatus.NEEDS_INTENT
