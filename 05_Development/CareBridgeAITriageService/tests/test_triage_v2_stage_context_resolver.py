"""P2-T6 stage/context graph-node tests."""

from __future__ import annotations

import pytest

from app.context import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.triage_v2.stage_context_resolver import stage_context_resolver
from app.triage_v2.state import create_initial_state


def _state():
    state = create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message="structured context",
    )
    state["intent"] = IntentType.SYMPTOM_TRIAGE
    return state


@pytest.mark.parametrize(
    ("entity", "field", "value", "expected"),
    [
        (TargetEntity.MOTHER, "gestationalWeek", 20, CareStage.PREGNANCY),
        (TargetEntity.MOTHER, "postpartumDay", 5, CareStage.POSTPARTUM_MOTHER),
        (TargetEntity.BABY, "babyAgeMonths", 3, CareStage.INFANT_0_12M),
        (TargetEntity.BABY, "babyAgeMonths", 18, CareStage.TODDLER_12_24M),
    ],
)
def test_stage_is_derived_only_inside_the_resolved_entity(entity, field, value, expected):
    state = _state()
    state["targetEntity"] = entity
    state[field] = value

    updates = stage_context_resolver(state)

    assert updates["stage"] is expected
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.RESOLVED


def test_possible_pregnancy_yes_is_not_collapsed_to_absent_or_preconception():
    state = _state()
    state["targetEntity"] = TargetEntity.MOTHER
    state["possiblePregnancy"] = "YES"

    assert stage_context_resolver(state)["stage"] is CareStage.POSSIBLE_PREGNANCY


@pytest.mark.parametrize(
    ("entity", "journey_stage"),
    [
        (TargetEntity.BABY, CareStage.PREGNANCY),
        (TargetEntity.MOTHER, CareStage.INFANT_0_12M),
    ],
)
def test_invalid_entity_stage_is_a_conflict_not_a_silent_rewrite(entity, journey_stage):
    state = _state()
    state["targetEntity"] = entity
    state["stage"] = journey_stage

    updates = stage_context_resolver(state)

    assert updates["stage"] is CareStage.CONFLICTED
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.CONFLICTED
    assert updates["contextConflicts"]


def test_explicit_baby_target_is_not_overridden_by_maternal_journey_context():
    state = _state()
    state["targetEntity"] = TargetEntity.BABY
    state["stage"] = CareStage.POSTPARTUM_MOTHER

    updates = stage_context_resolver(state)

    assert updates["stage"] is CareStage.CONFLICTED
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.CONFLICTED


def test_general_information_is_resolved_without_a_stage():
    state = _state()
    state["targetEntity"] = TargetEntity.MOTHER
    state["intent"] = IntentType.GENERAL_HEALTH_INFORMATION

    updates = stage_context_resolver(state)

    assert updates["stage"] is CareStage.UNKNOWN
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.RESOLVED


def test_unknown_target_blocks_stage_resolution():
    updates = stage_context_resolver(_state())

    assert updates["stage"] is CareStage.UNKNOWN
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.NEEDS_TARGET_ENTITY


def test_stopped_red_bypasses_context_resolution():
    state = _state()
    state.update(triageOutcome="RED", requiredAction="IMMEDIATE", stopConversation=True)

    assert stage_context_resolver(state) == {}
