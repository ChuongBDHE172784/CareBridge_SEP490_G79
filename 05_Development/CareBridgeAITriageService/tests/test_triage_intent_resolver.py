"""P2-T5 deterministic intent graph-node tests."""

from __future__ import annotations

import pytest

from app.context import IntentType, ResolutionSource
from app.triage.intent_resolver import intent_resolver
from app.triage.state import create_initial_state


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
    expected_keys = {"intent", "intentSource"}
    if expected not in {IntentType.UNKNOWN, IntentType.FOLLOW_UP_ANSWER}:
        expected_keys.add("confirmedConversationIntent")
    assert set(updates) == expected_keys


def test_fixed_catalog_option_is_structural_follow_up():
    updates = intent_resolver(_state("CLARIFY_TARGET_MOTHER"))

    assert updates["intent"] is IntentType.FOLLOW_UP_ANSWER
    assert updates["intentSource"] is ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER


def test_ambiguous_follow_up_keeps_confirmed_triage_intent():
    state = _state("không biết")
    state["intent"] = IntentType.SYMPTOM_TRIAGE

    updates = intent_resolver(state)

    assert updates["intent"] is IntentType.SYMPTOM_TRIAGE
    assert updates["intentSource"] is ResolutionSource.CONFIRMED_CONVERSATION_INTENT


def test_explicit_latest_intent_still_replaces_confirmed_intent():
    state = _state("Nguồn này từ đâu vậy?")
    state["intent"] = IntentType.SYMPTOM_TRIAGE

    updates = intent_resolver(state)

    assert updates["intent"] is IntentType.SOURCE_LOOKUP
    assert updates["intentSource"] is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE


def test_structural_answer_does_not_replace_the_confirmed_conversation_intent():
    state = _state("UNSURE")
    state["intent"] = IntentType.SYMPTOM_TRIAGE
    state["confirmedConversationIntent"] = IntentType.SYMPTOM_TRIAGE
    state["submittedOptionCodes"] = ["UNSURE"]

    answered = {**state, **intent_resolver(state)}
    assert answered["intent"] is IntentType.FOLLOW_UP_ANSWER

    answered["latestUserMessage"] = "khong biet"
    answered["submittedOptionCodes"] = []
    follow_up = intent_resolver(answered)

    assert follow_up["intent"] is IntentType.SYMPTOM_TRIAGE
    assert follow_up["intentSource"] is ResolutionSource.CONFIRMED_CONVERSATION_INTENT


def test_valid_text_reported_temperature_is_deterministic_symptom_triage_evidence():
    state = _state("Be hai thang do duoc 38,2 do")
    state["measurements"] = {
        "temperatureC": {
            "value": 38.2,
            "unit": "C",
            "temporalStatus": "CURRENT",
            "provenance": "USER_REPORTED_TEXT",
        }
    }

    updates = intent_resolver(state)

    assert updates["intent"] is IntentType.SYMPTOM_TRIAGE
    assert updates["intentSource"] is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE
    assert updates["confirmedConversationIntent"] is IntentType.SYMPTOM_TRIAGE


def test_text_reported_age_alone_does_not_invent_a_triage_intent():
    state = _state("Be hai thang tuoi")
    state["measurements"] = {
        "babyAgeMonths": {
            "value": 2,
            "unit": "MONTHS",
            "temporalStatus": "CURRENT",
            "provenance": "USER_REPORTED_TEXT",
        }
    }

    assert intent_resolver(state)["intent"] is IntentType.UNKNOWN


def test_new_current_temperature_overrides_prior_confirmed_information_intent():
    state = _state("Be hai thang do duoc 38,2 do")
    state["confirmedConversationIntent"] = IntentType.GENERAL_HEALTH_INFORMATION
    state["measurements"] = {
        "temperatureC": {
            "value": 38.2,
            "unit": "C",
            "temporalStatus": "CURRENT",
            "provenance": "USER_REPORTED_TEXT",
        }
    }

    updates = intent_resolver(state)

    assert updates["intent"] is IntentType.SYMPTOM_TRIAGE
    assert updates["intentSource"] is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE


def test_hypothetical_temperature_does_not_override_information_intent():
    state = _state("Neu be hai thang sot 38 do thi sao?")
    state["confirmedConversationIntent"] = IntentType.GENERAL_HEALTH_INFORMATION
    # A stale prior observation must not make this latest hypothetical statement explicit.
    state["measurements"] = {
        "temperatureC": {
            "value": 38.2,
            "unit": "C",
            "temporalStatus": "CURRENT",
            "provenance": "USER_REPORTED_TEXT",
        }
    }

    assert intent_resolver(state)["intent"] is IntentType.GENERAL_HEALTH_INFORMATION


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
