"""P2-T5 deterministic intent graph-node tests."""

from __future__ import annotations

import pytest

from app.context import IntentType, ResolutionSource
from app.triage_v2.intent_resolver import intent_resolver
from app.triage_v2.state import create_initial_state


def _state(message: str):
    return create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message=message,
    )


@pytest.mark.parametrize(
    ("message", "expected"),
    [
        ("Tôi bị ra máu từ sáng", IntentType.SYMPTOM_TRIAGE),
        ("Dấu hiệu cảnh báo thai kỳ là gì?", IntentType.GENERAL_HEALTH_INFORMATION),
        ("Nguồn này từ đâu vậy?", IntentType.SOURCE_LOOKUP),
        ("Cấp cứu, phải làm gì ngay?", IntentType.EMERGENCY_HELP),
        ("Kê đơn thuốc giúp tôi", IntentType.OUT_OF_SCOPE_REQUEST),
        ("ok", IntentType.UNKNOWN),
        ("CLARIFY_TARGET_BABY", IntentType.FOLLOW_UP_ANSWER),
    ],
)
def test_intent_families_are_resolved_without_a_clinical_result(message, expected):
    updates = intent_resolver(_state(message))

    assert updates["intent"] is expected
    assert set(updates) == {"intent", "intentSource"}


def test_fixed_catalog_option_is_structural_follow_up():
    updates = intent_resolver(_state("CLARIFY_TARGET_MOTHER"))

    assert updates["intent"] is IntentType.FOLLOW_UP_ANSWER
    assert updates["intentSource"] is ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER


@pytest.mark.parametrize(
    "message", ["Dấu hiệu cảnh báo thai kỳ là gì?", "Nguồn này từ đâu vậy?"]
)
def test_information_intents_never_emit_color_action_or_stop(message):
    updates = intent_resolver(_state(message))

    assert "triageOutcome" not in updates
    assert "requiredAction" not in updates
    assert "stopConversation" not in updates


def test_stopped_red_bypasses_intent_routing():
    state = _state("Nguồn này từ đâu vậy?")
    state.update(triageOutcome="RED", requiredAction="IMMEDIATE", stopConversation=True)

    assert intent_resolver(state) == {}
