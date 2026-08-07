"""P2-T3 deterministic global-safety-gate tests."""

from __future__ import annotations

from copy import deepcopy

import pytest

from app.rules.evaluator import DatasetStatus, PendingRiskStatus
from app.triage_v2.global_safety_gate import global_safety_gate
from app.triage_v2.state import create_initial_state


def _state():
    return create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message="health text must not appear in errors",
    )


@pytest.mark.parametrize(
    ("signal", "expected_id", "expected_action"),
    [
        ("SEIZURE", "GLOBAL_RED_001", "IMMEDIATE_EMERGENCY_ASSESSMENT"),
        ("SEVERE_BREATHING_DIFFICULTY", "GLOBAL_RED_001", "IMMEDIATE_EMERGENCY_ASSESSMENT"),
        ("CYANOSIS", "SAFETY_CYANOSIS_HOLDOVER_001", "IMMEDIATE_EMERGENCY_ASSESSMENT"),
        ("SELF_HARM_IDEATION", "SAFETY_SELF_HARM_001", "IMMEDIATE_SAFETY_SUPPORT"),
    ],
)
def test_explicit_global_danger_is_red_before_target_resolution(signal, expected_id, expected_action):
    state = _state()
    state["signals"][signal] = {"presence": "PRESENT"}
    before = deepcopy(state)

    updates = global_safety_gate(state)

    assert updates["triageOutcome"] == "RED"
    assert updates["stopConversation"] is True
    assert updates["requiredAction"] == expected_action
    assert expected_id in updates["decisiveRuleIds"]
    assert updates["finalResponse"] is None
    assert updates["readingLinks"] == []
    assert state == before


def test_unknown_global_screen_never_becomes_green_or_out_of_scope():
    state = _state()
    state.update(triageOutcome="GREEN", requiredAction="SELF_CARE", stopConversation=True)
    updates = global_safety_gate(state)
    merged = {**state, **updates}

    assert merged["triageOutcome"] is None
    assert merged["requiredAction"] is None
    assert merged["stopConversation"] is False
    assert updates["safetyScreenStatus"] is DatasetStatus.INCOMPLETE
    assert PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN in updates["pendingRiskStatuses"]


def test_complete_negative_global_screen_still_does_not_assign_an_outcome():
    state = _state()
    for signal in (
        "ALTERED_CONSCIOUSNESS", "SEIZURE", "SEVERE_BREATHING_DIFFICULTY", "CYANOSIS",
        "SELF_HARM_IDEATION", "SELF_HARM_INTENT_OR_PLAN", "HARM_TO_BABY_IDEATION",
        "CANNOT_ENSURE_OWN_SAFETY",
    ):
        state["signals"][signal] = {"presence": "ABSENT"}

    updates = global_safety_gate(state)

    assert updates["safetyScreenStatus"] is DatasetStatus.COMPLETE
    assert updates["triageOutcome"] is None
    assert "scopeStatus" not in updates


@pytest.mark.parametrize("value", [True, "UNKNOWN", {"presence": "CONFLICTED"}])
def test_untrusted_boolean_and_unresolved_presence_do_not_trigger_red(value):
    state = _state()
    state["signals"]["SEIZURE"] = value

    updates = global_safety_gate(state)

    assert updates["triageOutcome"] is None
    assert updates["safetyScreenStatus"] in {DatasetStatus.INCOMPLETE, DatasetStatus.CONFLICTED}


def test_historical_danger_is_not_promoted_to_current_but_current_observation_list_is_seen():
    historical = _state()
    historical["signals"]["SEIZURE"] = {
        "presence": "PRESENT",
        "temporalStatus": "HISTORICAL",
    }
    assert global_safety_gate(historical)["triageOutcome"] is None

    current = _state()
    current["signals"]["SEIZURE"] = [
        {"presence": "UNKNOWN"},
        {"presence": "PRESENT", "temporalStatus": "CURRENT"},
    ]
    assert global_safety_gate(current)["triageOutcome"] == "RED"


def test_registry_failure_is_controlled_stopped_and_never_green(monkeypatch):
    def fail():
        raise RuntimeError("caller health text must not leak")

    monkeypatch.setattr("app.triage_v2.global_safety_gate.get_registry", fail)
    updates = global_safety_gate(_state())

    assert updates["triageOutcome"] == "NEEDS_MORE_INFO"
    assert updates["requiredAction"] == "SYSTEM_UNAVAILABLE"
    assert updates["stopConversation"] is True
    assert str(updates["processingErrors"]) == "[{'code': 'RULE_REGISTRY_UNAVAILABLE', 'node': 'global_safety_gate', 'message': 'unavailable'}]"


@pytest.mark.parametrize("stop", [False, True])
def test_existing_red_is_monotonic_even_if_registry_later_fails(monkeypatch, stop):
    state = _state()
    state.update(triageOutcome="RED", requiredAction="IMMEDIATE", stopConversation=stop)
    monkeypatch.setattr(
        "app.triage_v2.global_safety_gate.get_registry",
        lambda: (_ for _ in ()).throw(RuntimeError("unavailable")),
    )

    updates = global_safety_gate(state)
    merged = {**state, **updates}
    assert merged["triageOutcome"] == "RED"
    assert merged["requiredAction"] == "IMMEDIATE"
    assert merged["stopConversation"] is True
