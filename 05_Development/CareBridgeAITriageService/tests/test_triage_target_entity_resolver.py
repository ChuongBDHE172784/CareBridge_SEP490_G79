"""P2-T4 target-entity graph-node tests."""

from __future__ import annotations

from copy import deepcopy

import pytest

from app.context import CareStage, ContextResolutionStatus, ResolutionSource, TargetEntity
from app.triage.state import create_initial_state
from app.triage.target_entity_resolver import target_entity_resolver


def _state(message: str):
    return create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message=message,
    )


@pytest.mark.parametrize(
    ("message", "entity"),
    [("Tôi bị tiêu chảy", TargetEntity.MOTHER), ("Bé bị tiêu chảy", TargetEntity.BABY)],
)
def test_explicit_subject_resolves_without_creating_clinical_output(message, entity):
    state = _state(message)
    before = deepcopy(state)

    updates = target_entity_resolver(state)

    assert updates["targetEntity"] is entity
    assert updates["targetEntitySource"] is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.INSUFFICIENT_CONTEXT
    assert "triageOutcome" not in updates
    assert state == before


def test_ambiguous_fever_in_postpartum_context_remains_unknown():
    state = _state("Bị sốt 38.5 độ")
    state["stage"] = CareStage.POSTPARTUM_MOTHER

    updates = target_entity_resolver(state)

    assert updates["targetEntity"] is TargetEntity.UNKNOWN
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.NEEDS_TARGET_ENTITY


def test_mother_and_baby_report_is_conflicted_not_silently_selected():
    updates = target_entity_resolver(_state("Tôi và bé đều bị sốt"))

    assert updates["targetEntity"] is TargetEntity.CONFLICTED
    assert updates["contextResolutionStatus"] is ContextResolutionStatus.CONFLICTED
    assert updates["contextConflicts"] == ["TARGET_ENTITY_CONFLICTED"]


def test_explicit_clarification_option_has_highest_precedence():
    state = _state("CLARIFY_TARGET_BABY")
    state["targetEntity"] = TargetEntity.MOTHER

    updates = target_entity_resolver(state)

    assert updates["targetEntity"] is TargetEntity.BABY
    assert updates["targetEntitySource"] is ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER


def test_ambiguous_follow_up_preserves_a_confirmed_conversation_target():
    state = _state("Vẫn còn mệt")
    state["targetEntity"] = TargetEntity.MOTHER

    updates = target_entity_resolver(state)

    assert updates["targetEntity"] is TargetEntity.MOTHER
    assert updates["targetEntitySource"] is ResolutionSource.CONFIRMED_CONVERSATION_TARGET


def test_explicit_message_overrides_selected_profile_but_ambiguous_message_uses_selection():
    explicit = _state("Toi bi tieu chay")
    explicit["targetEntity"] = TargetEntity.BABY
    explicit["targetEntitySource"] = ResolutionSource.EXPLICIT_SELECTED_PROFILE
    ambiguous = _state("Khong ro")
    ambiguous["targetEntity"] = TargetEntity.BABY
    ambiguous["targetEntitySource"] = ResolutionSource.EXPLICIT_SELECTED_PROFILE

    explicit_update = target_entity_resolver(explicit)
    ambiguous_update = target_entity_resolver(ambiguous)

    assert explicit_update["targetEntity"] is TargetEntity.MOTHER
    assert explicit_update["targetEntitySource"] is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE
    assert ambiguous_update["targetEntity"] is TargetEntity.BABY
    assert ambiguous_update["targetEntitySource"] is ResolutionSource.EXPLICIT_SELECTED_PROFILE


def test_stopped_red_bypasses_target_resolution():
    state = _state("Bé bị tiêu chảy")
    state.update(triageOutcome="RED", requiredAction="IMMEDIATE", stopConversation=True)

    assert target_entity_resolver(state) == {}
