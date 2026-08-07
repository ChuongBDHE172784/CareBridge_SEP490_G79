"""P2-T1 tests for the deterministic LangGraph state contract."""

from __future__ import annotations

import json

import pytest

import app.triage_v2 as triage_v2
from app.context import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
)
from app.rules.evaluator import DatasetStatus, MatchedRuleTrace, ScopeStatus
from app.triage_v2 import TriageV2State, create_initial_state
from app.triage_v2.state import MAXIMUM_QUESTION_ROUNDS, matched_rule_trace_to_audit


EXPECTED_STATE_KEYS = {
    "sessionId",
    "stateVersion",
    "expectedStateVersion",
    "requestId",
    "messageId",
    "processedRequestIds",
    "processedMessageIds",
    "rawMessages",
    "latestUserMessage",
    "targetEntity",
    "targetEntitySource",
    "intent",
    "intentSource",
    "stage",
    "contextResolutionStatus",
    "contextConflicts",
    "activeProfileId",
    "possiblePregnancy",
    "gestationalWeek",
    "postpartumDay",
    "babyAgeMonths",
    "signals",
    "measurements",
    "dataConflicts",
    "answeredQuestionIds",
    "unknownFields",
    "safetyScreenStatus",
    "contextDatasetStatus",
    "greenEligibilityDatasetStatus",
    "scopeStatus",
    "pendingRiskStatuses",
    "primaryPendingRiskStatus",
    "completionReason",
    "missingRequiredFields",
    "candidateQuestionIds",
    "plannedQuestionIds",
    "questionRound",
    "maximumQuestionRounds",
    "decisiveRuleIds",
    "allMatchedRules",
    "triageOutcome",
    "requiredAction",
    "reasonCodes",
    "stopConversation",
    "rulesetVersion",
    "rulesetHash",
    "processingErrors",
    "finalResponse",
    "readingLinks",
}


def _state(*, raw_messages=()):
    return create_initial_state(
        session_id="session-123",
        state_version=7,
        request_id="request-456",
        message_id="message-789",
        latest_user_message="Tôi cần được hỗ trợ",
        raw_messages=raw_messages,
        active_profile_id="profile-001",
    )


def test_typed_contract_requires_every_minimum_phase_2_field_without_freezing_extensions():
    # The Phase 2 prompt explicitly says not to lock the contract to exactly 42 fields;
    # later phases add transport-safe fields such as verified citations.
    assert EXPECTED_STATE_KEYS <= TriageV2State.__required_keys__
    assert TriageV2State.__optional_keys__ == frozenset()
    assert set(_state()) == TriageV2State.__required_keys__


def test_package_exports_only_the_state_contract_and_initializer():
    assert triage_v2.__all__ == ["TriageV2State", "create_initial_state"]


def test_initializer_preserves_request_identity_and_caller_context():
    state = _state(raw_messages=[{"role": "USER", "content": "Xin chào"}])

    assert state["sessionId"] == "session-123"
    assert state["stateVersion"] == 7
    assert state["expectedStateVersion"] == 7
    assert state["requestId"] == "request-456"
    assert state["messageId"] == "message-789"
    assert state["latestUserMessage"] == "Tôi cần được hỗ trợ"
    assert state["rawMessages"] == [{"role": "USER", "content": "Xin chào"}]
    assert state["activeProfileId"] == "profile-001"


def test_initializer_uses_explicit_unresolved_fail_safe_defaults():
    state = _state()

    assert state["targetEntity"] is TargetEntity.UNKNOWN
    assert state["targetEntitySource"] is ResolutionSource.NONE
    assert state["intent"] is IntentType.UNKNOWN
    assert state["intentSource"] is ResolutionSource.NONE
    assert state["stage"] is CareStage.UNKNOWN
    assert state["contextResolutionStatus"] is ContextResolutionStatus.INSUFFICIENT_CONTEXT
    assert state["possiblePregnancy"] == "UNKNOWN"
    assert state["gestationalWeek"] is None
    assert state["postpartumDay"] is None
    assert state["babyAgeMonths"] is None
    assert state["safetyScreenStatus"] is DatasetStatus.INCOMPLETE
    assert state["contextDatasetStatus"] is DatasetStatus.INCOMPLETE
    assert state["greenEligibilityDatasetStatus"] is DatasetStatus.INCOMPLETE
    assert state["scopeStatus"] is ScopeStatus.UNKNOWN
    assert state["primaryPendingRiskStatus"] is None
    assert state["completionReason"] is None


def test_initializer_never_invents_an_outcome_action_or_evidence():
    state = _state()

    assert state["triageOutcome"] is None
    assert state["requiredAction"] is None
    assert state["stopConversation"] is False
    assert state["rulesetVersion"] is None
    assert state["rulesetHash"] is None
    assert state["processingErrors"] == []
    assert state["finalResponse"] is None
    assert state["readingLinks"] == []
    assert state["questionRound"] == 0
    assert state["maximumQuestionRounds"] == MAXIMUM_QUESTION_ROUNDS == 3


def test_states_do_not_share_mutable_containers():
    first = _state()
    second = _state()

    for key in (
        "rawMessages",
        "processedRequestIds",
        "processedMessageIds",
        "contextConflicts",
        "signals",
        "measurements",
        "dataConflicts",
        "answeredQuestionIds",
        "unknownFields",
        "pendingRiskStatuses",
        "missingRequiredFields",
        "candidateQuestionIds",
        "plannedQuestionIds",
        "decisiveRuleIds",
        "reasonCodes",
        "allMatchedRules",
        "processingErrors",
        "readingLinks",
    ):
        assert first[key] is not second[key], key

    first["signals"]["SEIZURE"] = {"presence": "PRESENT"}
    first["readingLinks"].append("must-not-leak")
    assert second["signals"] == {}
    assert second["readingLinks"] == []


def test_initializer_defensively_copies_nested_raw_messages():
    caller_messages = [
        {"role": "USER", "content": "Xin chào", "metadata": {"turn": 1}}
    ]
    state = _state(raw_messages=caller_messages)

    caller_messages[0]["content"] = "changed"
    caller_messages[0]["metadata"]["turn"] = 2
    caller_messages.append({"role": "ASSISTANT", "content": "new"})

    assert state["rawMessages"] == [
        {"role": "USER", "content": "Xin chào", "metadata": {"turn": 1}}
    ]


@pytest.mark.parametrize(
    "invalid_value",
    [
        {"not-json"},
        float("nan"),
        {1: "non-string-key"},
    ],
)
def test_initializer_rejects_non_json_message_values(invalid_value):
    with pytest.raises((TypeError, ValueError)):
        _state(raw_messages=[{"role": "USER", "metadata": invalid_value}])


def test_state_remains_standard_json_serializable_with_rule_audit_traces():
    state = _state(raw_messages=[{"role": "USER", "content": "Xin chào"}])
    state["allMatchedRules"].append(
        matched_rule_trace_to_audit(
            MatchedRuleTrace(
                rule_id="RULE-001",
                outcome="RED",
                priority=1,
                rule_version="1.0.0",
                role="SUPPRESSED_BY_HIGHER_PRIORITY",
                suppression_reason="EXPLICIT_RULE_EXCLUSION",
                missing_fields=("current",),
            )
        )
    )

    restored = json.loads(json.dumps(state, ensure_ascii=False, allow_nan=False))

    assert restored["targetEntity"] == "UNKNOWN"
    assert restored["allMatchedRules"] == [
        {
            "ruleId": "RULE-001",
            "outcome": "RED",
            "priority": 1,
            "ruleVersion": "1.0.0",
            "role": "SUPPRESSED_BY_HIGHER_PRIORITY",
            "suppressionReason": "EXPLICIT_RULE_EXCLUSION",
            "missingFields": ["current"],
        }
    ]
