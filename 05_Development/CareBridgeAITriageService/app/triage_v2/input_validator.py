"""Deterministic, side-effect-free input boundary for AI Triage V2."""

from __future__ import annotations

import re
import unicodedata
from enum import Enum
from math import isfinite
from typing import Mapping

from app.context import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
)
from app.rules.evaluator import (
    CompletionReason,
    DatasetStatus,
    PendingRiskStatus,
    ScopeStatus,
)
from app.triage_v2.state import MAXIMUM_QUESTION_ROUNDS, TriageV2State

MAX_ID_LENGTH = 128
MAX_MESSAGE_LENGTH = 4_000
MAX_GENERIC_STRING_LENGTH = 8_192
MAX_JSON_KEY_LENGTH = 256
MAX_TOTAL_JSON_CHARACTERS = 262_144
MAX_JSON_INTEGER_BITS = 256
MAX_JSON_DEPTH = 10
MAX_JSON_NODES = 4_096
MAX_STATE_VERSION = 2**63 - 1
MAX_CONTEXT_NUMBER = 1_000_000

_ID_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._:-]*$")
_ALLOWED_TEXT_CONTROLS = frozenset({"\n", "\t"})
_BIDI_CONTROL_CHARACTERS = frozenset(
    chr(codepoint)
    for codepoint in (*range(0x202A, 0x202F), *range(0x2066, 0x206A))
)
_POSSIBLE_PREGNANCY = frozenset({"YES", "NO", "UNKNOWN", "CONFLICTED"})
_TRIAGE_OUTCOMES = frozenset({"RED", "YELLOW", "GREEN", "NEEDS_MORE_INFO", "OUT_OF_SCOPE"})
_RAW_MESSAGE_KEYS = frozenset({"role", "content", "messageId", "metadata"})
_RAW_MESSAGE_ROLES = frozenset({"USER", "ASSISTANT", "SYSTEM"})

_ENUM_FIELDS: dict[str, type[Enum]] = {
    "targetEntity": TargetEntity,
    "targetEntitySource": ResolutionSource,
    "intent": IntentType,
    "intentSource": ResolutionSource,
    "stage": CareStage,
    "contextResolutionStatus": ContextResolutionStatus,
    "safetyScreenStatus": DatasetStatus,
    "contextDatasetStatus": DatasetStatus,
    "greenEligibilityDatasetStatus": DatasetStatus,
    "scopeStatus": ScopeStatus,
}

_OPTIONAL_ENUM_FIELDS: dict[str, type[Enum]] = {
    "primaryPendingRiskStatus": PendingRiskStatus,
    "completionReason": CompletionReason,
}

_LIST_LIMITS = {
    "rawMessages": 36,
    "processedRequestIds": 256,
    "processedMessageIds": 256,
    "contextConflicts": 64,
    "dataConflicts": 64,
    "answeredQuestionIds": 36,
    "unknownFields": 128,
    "pendingRiskStatuses": 16,
    "missingRequiredFields": 128,
    "candidateQuestionIds": 128,
    "plannedQuestionIds": 3,
    "decisiveRuleIds": 64,
    "reasonCodes": 128,
    "allMatchedRules": 256,
    "processingErrors": 16,
    "citations": 4,
    "readingLinks": 0,
}

_DICT_LIMITS = {
    "signals": 128,
    "measurements": 64,
}

_STRING_LIST_FIELDS = frozenset(
    {
        "contextConflicts",
        "dataConflicts",
        "answeredQuestionIds",
        "processedRequestIds",
        "processedMessageIds",
        "unknownFields",
        "missingRequiredFields",
        "candidateQuestionIds",
        "plannedQuestionIds",
        "decisiveRuleIds",
        "reasonCodes",
        "readingLinks",
    }
)


class InputValidationError(ValueError):
    """Controlled validation failure containing no caller-supplied value."""

    def __init__(self, code: str, field: str) -> None:
        self.code = code
        self.field = field
        super().__init__(f"{code}:{field}")


def input_validator(state: Mapping[str, object]) -> dict[str, object]:
    """Validate one graph entry and return only safe enum-normalization updates.

    The function never mutates ``state``, never logs input, and never writes clinical
    output. Freshness/idempotency and semantic context validation belong to later nodes.
    """

    if type(state) is not dict:
        _fail("INVALID_STATE", "state")
    _require_complete_state(state)

    for field in ("sessionId", "requestId", "messageId"):
        _validate_identifier(state[field], field)
    active_profile_id = state["activeProfileId"]
    if active_profile_id is not None:
        _validate_identifier(active_profile_id, "activeProfileId")
    _validate_latest_message(state["latestUserMessage"])

    _validate_integer(state["stateVersion"], "stateVersion", maximum=MAX_STATE_VERSION)
    _validate_integer(
        state["expectedStateVersion"], "expectedStateVersion", maximum=MAX_STATE_VERSION
    )
    _validate_integer(
        state["questionRound"],
        "questionRound",
        maximum=MAXIMUM_QUESTION_ROUNDS,
    )
    _validate_exact_integer(
        state["maximumQuestionRounds"],
        "maximumQuestionRounds",
        expected=MAXIMUM_QUESTION_ROUNDS,
    )
    for field in ("gestationalWeek", "postpartumDay", "babyAgeMonths"):
        value = state[field]
        if value is not None:
            _validate_integer(value, field, maximum=MAX_CONTEXT_NUMBER)

    possible_pregnancy = state["possiblePregnancy"]
    if type(possible_pregnancy) is not str or possible_pregnancy not in _POSSIBLE_PREGNANCY:
        _fail("INVALID_ENUM", "possiblePregnancy")

    outcome = state["triageOutcome"]
    if outcome is not None and (type(outcome) is not str or outcome not in _TRIAGE_OUTCOMES):
        _fail("INVALID_ENUM", "triageOutcome")
    if type(state["stopConversation"]) is not bool:
        _fail("INVALID_BOOLEAN", "stopConversation")

    for field, limit in _LIST_LIMITS.items():
        _validate_list(state[field], field, maximum=limit)
    for field, limit in _DICT_LIMITS.items():
        _validate_dict(state[field], field, maximum=limit)
    for field in _STRING_LIST_FIELDS:
        _validate_string_list(state[field], field)

    raw_messages = state["rawMessages"]
    for message in raw_messages:
        _validate_raw_message(message)

    for field in ("allMatchedRules", "processingErrors", "citations"):
        for item in state[field]:
            if type(item) is not dict:
                _fail("INVALID_COLLECTION_ITEM", field)

    # Run the generic JSON-safety walk only after known fields have produced
    # their more actionable boundary errors.
    _validate_json_tree(state, field="state")

    updates: dict[str, object] = {}
    for field, enum_type in _ENUM_FIELDS.items():
        normalized = _strict_enum(state[field], field, enum_type)
        if normalized is not state[field]:
            updates[field] = normalized

    for field, enum_type in _OPTIONAL_ENUM_FIELDS.items():
        value = state[field]
        if value is None:
            continue
        normalized = _strict_enum(value, field, enum_type)
        if normalized is not value:
            updates[field] = normalized

    pending = state["pendingRiskStatuses"]
    normalized_pending = [
        _strict_enum(item, "pendingRiskStatuses", PendingRiskStatus) for item in pending
    ]
    if any(normalized is not original for normalized, original in zip(normalized_pending, pending)):
        updates["pendingRiskStatuses"] = normalized_pending

    return updates


def _require_complete_state(state: Mapping[str, object]) -> None:
    for field in TriageV2State.__required_keys__:
        if field not in state:
            _fail("MISSING_FIELD", field)


def _validate_identifier(value: object, field: str) -> None:
    if type(value) is not str:
        _fail("INVALID_ID", field)
    if not value or len(value) > MAX_ID_LENGTH or _ID_PATTERN.fullmatch(value) is None:
        _fail("INVALID_ID", field)


def _validate_latest_message(value: object) -> None:
    _validate_text(value, "latestUserMessage", allow_blank=False)


def _validate_text(value: object, field: str, *, allow_blank: bool) -> None:
    if type(value) is not str:
        _fail("INVALID_TEXT", field)
    if len(value) > MAX_MESSAGE_LENGTH:
        _fail("TEXT_TOO_LONG", field)
    if not allow_blank and not value.strip():
        _fail("BLANK_TEXT", field)
    if _contains_disallowed_control(value):
        _fail("CONTROL_CHARACTER", field)


def _validate_integer(value: object, field: str, *, maximum: int) -> None:
    if type(value) is not int:
        _fail("INVALID_INTEGER", field)
    if value < 0 or value > maximum:
        _fail("INTEGER_OUT_OF_RANGE", field)


def _validate_exact_integer(value: object, field: str, *, expected: int) -> None:
    _validate_integer(value, field, maximum=expected)
    if value != expected:
        _fail("INVALID_LIMIT", field)


def _validate_list(value: object, field: str, *, maximum: int) -> None:
    if type(value) is not list:
        _fail("INVALID_COLLECTION", field)
    if len(value) > maximum:
        _fail("COLLECTION_TOO_LARGE", field)


def _validate_dict(value: object, field: str, *, maximum: int) -> None:
    if type(value) is not dict:
        _fail("INVALID_COLLECTION", field)
    if len(value) > maximum:
        _fail("COLLECTION_TOO_LARGE", field)


def _validate_string_list(value: object, field: str) -> None:
    for item in value:
        if type(item) is not str or not item or len(item) > MAX_JSON_KEY_LENGTH:
            _fail("INVALID_COLLECTION_ITEM", field)
        if _contains_disallowed_control(item):
            _fail("INVALID_COLLECTION_ITEM", field)


def _validate_raw_message(value: object) -> None:
    if type(value) is not dict:
        _fail("INVALID_COLLECTION_ITEM", "rawMessages")
    if set(value) - _RAW_MESSAGE_KEYS:
        _fail("UNKNOWN_MESSAGE_FIELD", "rawMessages")
    if set(value) < {"role", "content"}:
        _fail("MISSING_MESSAGE_FIELD", "rawMessages")
    role = value["role"]
    if type(role) is not str or role not in _RAW_MESSAGE_ROLES:
        _fail("INVALID_MESSAGE_ROLE", "rawMessages")
    _validate_text(value["content"], "rawMessages", allow_blank=False)
    message_id = value.get("messageId")
    if message_id is not None:
        _validate_identifier(message_id, "rawMessages")
    metadata = value.get("metadata")
    if metadata is not None and type(metadata) is not dict:
        _fail("INVALID_MESSAGE_METADATA", "rawMessages")


def _strict_enum(value: object, field: str, enum_type: type[Enum]) -> Enum:
    if isinstance(value, enum_type):
        return value
    if type(value) is not str:
        _fail("INVALID_ENUM", field)
    try:
        return enum_type(value)
    except ValueError:
        _fail("INVALID_ENUM", field)


def _validate_json_tree(value: object, *, field: str) -> None:
    remaining = [MAX_JSON_NODES]
    remaining_characters = [MAX_TOTAL_JSON_CHARACTERS]
    _walk_json(
        value,
        field=field,
        depth=0,
        ancestors=set(),
        remaining=remaining,
        remaining_characters=remaining_characters,
    )


def _walk_json(
    value: object,
    *,
    field: str,
    depth: int,
    ancestors: set[int],
    remaining: list[int],
    remaining_characters: list[int],
) -> None:
    remaining[0] -= 1
    if remaining[0] < 0:
        _fail("JSON_TOO_COMPLEX", field)
    if depth > MAX_JSON_DEPTH:
        _fail("JSON_TOO_DEEP", field)
    if value is None or type(value) is bool:
        return
    if isinstance(value, str):
        if len(value) > MAX_GENERIC_STRING_LENGTH:
            _fail("JSON_STRING_TOO_LONG", field)
        remaining_characters[0] -= len(value)
        if remaining_characters[0] < 0:
            _fail("JSON_TOO_LARGE", field)
        if _contains_disallowed_control(value):
            _fail("CONTROL_CHARACTER", field)
        return
    if type(value) is int:
        if value.bit_length() > MAX_JSON_INTEGER_BITS:
            _fail("JSON_NUMBER_TOO_LARGE", field)
        return
    if type(value) is float:
        if not isfinite(value):
            _fail("NON_FINITE_NUMBER", field)
        return
    if type(value) is dict:
        identity = id(value)
        if identity in ancestors:
            _fail("CYCLIC_JSON", field)
        ancestors.add(identity)
        try:
            for key, item in value.items():
                if type(key) is not str:
                    _fail("NON_STRING_JSON_KEY", field)
                if len(key) > MAX_JSON_KEY_LENGTH or _contains_disallowed_control(key):
                    _fail("INVALID_JSON_KEY", field)
                remaining_characters[0] -= len(key)
                if remaining_characters[0] < 0:
                    _fail("JSON_TOO_LARGE", field)
                _walk_json(
                    item,
                    field=field,
                    depth=depth + 1,
                    ancestors=ancestors,
                    remaining=remaining,
                    remaining_characters=remaining_characters,
                )
        finally:
            ancestors.remove(identity)
        return
    if type(value) is list:
        identity = id(value)
        if identity in ancestors:
            _fail("CYCLIC_JSON", field)
        ancestors.add(identity)
        try:
            for item in value:
                _walk_json(
                    item,
                    field=field,
                    depth=depth + 1,
                    ancestors=ancestors,
                    remaining=remaining,
                    remaining_characters=remaining_characters,
                )
        finally:
            ancestors.remove(identity)
        return
    _fail("NON_JSON_VALUE", field)


def _contains_disallowed_control(value: str) -> bool:
    return any(
        (unicodedata.category(character) in {"Cc", "Cs"}
         and character not in _ALLOWED_TEXT_CONTROLS)
        or character in _BIDI_CONTROL_CHARACTERS
        for character in value
    )


def _fail(code: str, field: str):
    raise InputValidationError(code, field)
