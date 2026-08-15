"""P2-T2 deterministic input-validator tests."""

from __future__ import annotations

import json
from copy import deepcopy

import pytest

from app.context import TargetEntity
from app.triage import create_initial_state
from app.triage.input_validator import (
    MAX_ID_LENGTH,
    MAX_JSON_KEY_LENGTH,
    MAX_MESSAGE_LENGTH,
    MAX_TOTAL_JSON_CHARACTERS,
    InputValidationError,
    input_validator,
)


def _state():
    return create_initial_state(
        session_id="session-123",
        state_version=0,
        request_id="request-456",
        message_id="message-789",
        latest_user_message="Tôi cần được hỗ trợ",
        raw_messages=[{"role": "USER", "content": "Tôi cần được hỗ trợ"}],
    )


def _assert_rejected(state, code: str, field: str) -> InputValidationError:
    with pytest.raises(InputValidationError) as captured:
        input_validator(state)
    assert captured.value.code == code
    assert captured.value.field == field
    assert str(captured.value) == f"{code}:{field}"
    return captured.value


def test_valid_initial_state_returns_no_update_and_is_not_mutated():
    state = _state()
    before = deepcopy(state)

    assert input_validator(state) == {}
    assert state == before


def test_exact_json_enum_values_are_normalized_without_other_updates():
    state = json.loads(json.dumps(_state(), ensure_ascii=False))

    updates = input_validator(state)

    assert updates["targetEntity"] is TargetEntity.UNKNOWN
    assert set(updates) == {
        "targetEntity",
        "targetEntitySource",
            "intent",
            "intentSource",
            "confirmedConversationIntent",
            "stage",
        "contextResolutionStatus",
        "safetyScreenStatus",
        "contextDatasetStatus",
        "greenEligibilityDatasetStatus",
        "scopeStatus",
        "subjectScope",
        "complaintScope",
        "coverageStatus",
    }
    assert state["targetEntity"] == "UNKNOWN"


@pytest.mark.parametrize(
    "field,value",
    [
        ("sessionId", ""),
        ("requestId", "contains space"),
        ("messageId", "bad/slash"),
        ("sessionId", "a" * (MAX_ID_LENGTH + 1)),
        ("messageId", 123),
    ],
)
def test_invalid_identifiers_are_rejected(field, value):
    state = _state()
    state[field] = value
    _assert_rejected(state, "INVALID_ID", field)


@pytest.mark.parametrize("value", ["", "bad profile", "a" * (MAX_ID_LENGTH + 1), 123])
def test_active_profile_id_is_optional_but_strict_when_present(value):
    state = _state()
    state["activeProfileId"] = value
    _assert_rejected(state, "INVALID_ID", "activeProfileId")

    state = _state()
    state["activeProfileId"] = None
    assert input_validator(state) == {}


@pytest.mark.parametrize(
    "message,code",
    [
        ("", "BLANK_TEXT"),
        ("   \t\n", "BLANK_TEXT"),
        ("a" * (MAX_MESSAGE_LENGTH + 1), "TEXT_TOO_LONG"),
        ("dữ liệu\x00ẩn", "CONTROL_CHARACTER"),
        ("dữ liệu\u202eẩn", "CONTROL_CHARACTER"),
    ],
)
def test_invalid_latest_message_is_rejected_without_disclosure(message, code):
    state = _state()
    state["latestUserMessage"] = message
    error = _assert_rejected(state, code, "latestUserMessage")
    if message:
        assert message not in str(error)


@pytest.mark.parametrize(
    "field,value,code",
    [
        ("stateVersion", True, "INVALID_INTEGER"),
        ("stateVersion", 1.0, "INVALID_INTEGER"),
        ("stateVersion", -1, "INTEGER_OUT_OF_RANGE"),
        ("stateVersion", 2**63, "INTEGER_OUT_OF_RANGE"),
        ("questionRound", 4, "INTEGER_OUT_OF_RANGE"),
        ("maximumQuestionRounds", 2, "INVALID_LIMIT"),
        ("gestationalWeek", False, "INVALID_INTEGER"),
        ("postpartumDay", -1, "INTEGER_OUT_OF_RANGE"),
    ],
)
def test_integer_fields_are_strict_and_bounded(field, value, code):
    state = _state()
    state[field] = value
    _assert_rejected(state, code, field)


def test_stop_conversation_requires_an_exact_boolean():
    state = _state()
    state["stopConversation"] = 1
    _assert_rejected(state, "INVALID_BOOLEAN", "stopConversation")


@pytest.mark.parametrize(
    "field,value",
    [
        ("targetEntity", "unknown"),
        ("stage", "POSTPARTUM"),
        ("intent", "SYMPTOM"),
        ("possiblePregnancy", "MAYBE"),
        ("safetyScreenStatus", 1),
    ],
)
def test_enum_values_require_exact_canonical_membership(field, value):
    state = _state()
    state[field] = value
    _assert_rejected(state, "INVALID_ENUM", field)


def test_non_finite_and_opaque_values_are_rejected_as_json_unsafe():
    state = _state()
    state["measurements"]["temperature"] = float("nan")
    _assert_rejected(state, "NON_FINITE_NUMBER", "state")

    state = _state()
    state["signals"]["unsafe"] = {"opaque"}
    _assert_rejected(state, "NON_JSON_VALUE", "state")


def test_cyclic_and_oversized_collections_are_rejected():
    state = _state()
    cycle = {}
    cycle["self"] = cycle
    state["signals"]["cycle"] = cycle
    _assert_rejected(state, "CYCLIC_JSON", "state")

    state = _state()
    state["answeredQuestionIds"] = [f"Q-{index}" for index in range(37)]
    _assert_rejected(state, "COLLECTION_TOO_LARGE", "answeredQuestionIds")

    state = _state()
    state["askedQuestionIds"] = [f"Q-{index}" for index in range(37)]
    _assert_rejected(state, "COLLECTION_TOO_LARGE", "askedQuestionIds")


@pytest.mark.parametrize(
    "field,value",
    [
        ("signals", []),
        ("measurements", ()),
        ("contextConflicts", {}),
        ("answeredQuestionIds", {}),
        ("askedQuestionIds", {}),
        ("readingLinks", {}),
    ],
)
def test_collections_require_the_exact_runtime_shape(field, value):
    state = _state()
    state[field] = value
    _assert_rejected(state, "INVALID_COLLECTION", field)


def test_string_collections_and_audit_collections_validate_items():
    state = _state()
    state["answeredQuestionIds"] = [True]
    _assert_rejected(state, "INVALID_COLLECTION_ITEM", "answeredQuestionIds")

    state = _state()
    state["allMatchedRules"] = ["not-an-audit-object"]
    _assert_rejected(state, "INVALID_COLLECTION_ITEM", "allMatchedRules")


def test_raw_messages_use_a_closed_schema_and_may_contain_prior_history():
    state = _state()
    state["rawMessages"] = [{"role": "USER", "content": "different"}]
    assert input_validator(state) == {}

    state = _state()
    state["rawMessages"] = [{"role": "user", "content": state["latestUserMessage"]}]
    _assert_rejected(state, "INVALID_MESSAGE_ROLE", "rawMessages")

    state = _state()
    state["rawMessages"] = [{"role": "USER", "extra": "not allowed"}]
    _assert_rejected(state, "UNKNOWN_MESSAGE_FIELD", "rawMessages")

    state = _state()
    state["rawMessages"] = [{"role": "USER", "metadata": {}}]
    _assert_rejected(state, "MISSING_MESSAGE_FIELD", "rawMessages")

    state = _state()
    state["rawMessages"] = [{"content": "safe", "metadata": {}}]
    _assert_rejected(state, "MISSING_MESSAGE_FIELD", "rawMessages")


def test_only_builtin_json_containers_are_accepted():
    state = _state()
    state["signals"]["custom-sequence"] = range(3)
    _assert_rejected(state, "NON_JSON_VALUE", "state")


def test_json_key_integer_and_aggregate_character_budgets_are_enforced():
    state = _state()
    state["signals"]["k" * (MAX_JSON_KEY_LENGTH + 1)] = True
    _assert_rejected(state, "INVALID_JSON_KEY", "state")

    state = _state()
    state["signals"]["huge"] = 1 << 300
    _assert_rejected(state, "JSON_NUMBER_TOO_LARGE", "state")

    state = _state()
    chunk = "x" * 8_000
    state["signals"] = {str(index): chunk for index in range(33)}
    assert sum(len(value) for value in state["signals"].values()) > MAX_TOTAL_JSON_CHARACTERS
    _assert_rejected(state, "JSON_TOO_LARGE", "state")


def test_zero_width_joiner_is_allowed_but_bidi_override_and_surrogate_are_rejected():
    state = _state()
    state["latestUserMessage"] = "gia đình 👩\u200d⚕️"
    state["rawMessages"] = [{"role": "USER", "content": state["latestUserMessage"]}]
    assert input_validator(state) == {}

    state = _state()
    state["signals"]["unsafe"] = "abc\u202edef"
    _assert_rejected(state, "CONTROL_CHARACTER", "state")

    state = _state()
    state["signals"]["unsafe"] = "abc\ud800def"
    _assert_rejected(state, "CONTROL_CHARACTER", "state")


def test_phase_2_rejects_any_caller_supplied_reading_link():
    state = _state()
    state["readingLinks"] = ["https://example.invalid/not-allowed"]
    _assert_rejected(state, "COLLECTION_TOO_LARGE", "readingLinks")


def test_missing_required_state_field_is_rejected_without_fixed_key_count():
    state = _state()
    del state["requestId"]
    _assert_rejected(state, "MISSING_FIELD", "requestId")

    extended_state = _state()
    extended_state["futureJsonField"] = {"safe": True}
    assert input_validator(extended_state) == {}


def test_failure_never_changes_clinical_output_fields_or_logs_health_text(caplog):
    secret_text = "triệu chứng riêng tư 123"
    state = _state()
    state["latestUserMessage"] = secret_text + "\x00"
    before = deepcopy(state)

    error = _assert_rejected(state, "CONTROL_CHARACTER", "latestUserMessage")

    assert state == before
    assert state["triageOutcome"] is None
    assert state["requiredAction"] is None
    assert state["finalResponse"] is None
    assert state["readingLinks"] == []
    assert secret_text not in str(error)
    assert secret_text not in caplog.text
