"""Question planning, optimistic concurrency, and safe follow-up re-entry."""

from __future__ import annotations

from typing import Mapping, TypeVar

from langgraph.types import interrupt
from langgraph.config import get_config

from app.context import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.questions.catalog_filter import FilterContext, eligible_questions
from app.questions.planner import plan_questions
from app.rules.evaluator import CompletionReason
from app.triage_v2.coverage_resolver import clinical_questions_allowed
from app.triage_v2.global_safety_gate import global_safety_gate
from app.triage_v2.input_validator import input_validator
from app.triage_v2.state import MAXIMUM_QUESTION_ROUNDS

_EnumT = TypeVar("_EnumT")
_RESUME_REQUIRED_FIELDS = frozenset(
    {"requestId", "messageId", "latestUserMessage", "expectedStateVersion"}
)
_RESUME_ALLOWED_FIELDS = _RESUME_REQUIRED_FIELDS | {"signals", "measurements"}


def resume_safety_entry(
    state: Mapping[str, object], *, current_signal_delta: object = None
) -> dict[str, object]:
    """Re-enter through validation and global safety; planning is deliberately separate."""

    normalized = input_validator(state)
    validated = {**state, **normalized}
    if type(current_signal_delta) is dict:
        prior = state.get("signals") if type(state.get("signals")) is dict else {}
        validated["signals"] = {**prior, **current_signal_delta}
    return {**normalized, **global_safety_gate(validated)}


def idempotency_guard(state: Mapping[str, object]) -> dict[str, object]:
    """Stop this execution on stale/duplicate identity before any clinical mutation."""

    conflict = _concurrency_guard(state)
    if conflict is not None:
        return conflict
    if _is_duplicate(state):
        return {
            "requiredAction": "DUPLICATE_REQUEST",
            "candidateQuestionIds": [],
            "plannedQuestionIds": [],
            "finalResponse": None,
            "readingLinks": [],
        }
    return {}


def follow_up_interrupt(state: Mapping[str, object]) -> dict[str, object]:
    """Suspend a question turn and safely merge the next request on resume."""

    resumed = interrupt(
        {
            "kind": "TRIAGE_FOLLOW_UP_REQUIRED",
            "sessionId": state.get("sessionId"),
            "stateVersion": state.get("stateVersion"),
            "questionRound": state.get("questionRound"),
            "questionIds": list(state.get("plannedQuestionIds", [])),
        }
    )
    if type(resumed) is not dict:
        raise ValueError("INVALID_RESUME_PAYLOAD")
    if set(resumed) - _RESUME_ALLOWED_FIELDS or not _RESUME_REQUIRED_FIELDS <= set(resumed):
        raise ValueError("INVALID_RESUME_FIELDS")
    thread_id = get_config().get("configurable", {}).get("thread_id")
    if thread_id != state.get("sessionId"):
        raise ValueError("SESSION_THREAD_MISMATCH")

    safe_resume = {
        "requestId": resumed["requestId"],
        "messageId": resumed["messageId"],
        "latestUserMessage": resumed["latestUserMessage"],
        "expectedStateVersion": resumed["expectedStateVersion"],
        "signals": _merge_delta(state.get("signals"), resumed.get("signals")),
        "measurements": _merge_mapping(state.get("measurements"), resumed.get("measurements")),
    }
    merged = {**state, **safe_resume}
    return {
        **safe_resume,
        **resume_safety_entry(merged, current_signal_delta=resumed.get("signals")),
    }


def _merge_delta(current: object, delta: object) -> dict[str, object]:
    merged = dict(current) if type(current) is dict else {}
    if delta is None:
        return merged
    if type(delta) is not dict:
        raise ValueError("INVALID_RESUME_SIGNALS")
    for code, observation in delta.items():
        if code in merged:
            previous = merged[code]
            previous_items = previous if type(previous) is list else [previous]
            delta_items = observation if type(observation) is list else [observation]
            merged[code] = [*previous_items, *delta_items]
        else:
            merged[code] = observation
    return merged


def _merge_mapping(current: object, delta: object) -> dict[str, object]:
    merged = dict(current) if type(current) is dict else {}
    if delta is None:
        return merged
    if type(delta) is not dict:
        raise ValueError("INVALID_RESUME_MEASUREMENTS")
    merged.update(delta)
    return merged


def question_planner(state: Mapping[str, object]) -> dict[str, object]:
    """Plan one bounded fixed-catalog round after idempotency/concurrency guards."""

    if state.get("triageOutcome") in {"RED", "OUT_OF_SCOPE"} or state.get("stopConversation") is True:
        return {"candidateQuestionIds": [], "plannedQuestionIds": []}

    conflict = _concurrency_guard(state)
    if conflict is not None:
        return conflict

    if _is_duplicate(state):
        return {"plannedQuestionIds": [], "candidateQuestionIds": []}

    current_round = state.get("questionRound") if type(state.get("questionRound")) is int else 0
    if current_round >= MAXIMUM_QUESTION_ROUNDS:
        return {
            "plannedQuestionIds": [],
            "candidateQuestionIds": [],
            "requiredAction": "ROUTE_TO_HEALTHCARE_WORKER",
            "stopConversation": True,
            "completionReason": CompletionReason.MAX_QUESTION_ROUNDS_REACHED,
        }

    filtered = eligible_questions(
        _filter_context(state),
        state.get("candidateQuestionIds") if type(state.get("candidateQuestionIds")) is list else [],
    )
    filtered_ids = [question.question_id for question in filtered]

    # The clinical catalogue is not a fallback. Clarification questions stay available — they are
    # how an unresolved target gets resolved — but once coverage or intent rules out clinical
    # questioning we stop rather than reaching for whichever clinical question is closest. That
    # reach is how a wrist-pain complaint ended up being asked about vaginal bleeding.
    allowed, refusal = clinical_questions_allowed(state)
    if not allowed:
        clarification_ids = [
            question.question_id for question in filtered if question.is_clarification
        ]
        if clarification_ids:
            filtered_ids = clarification_ids
            filtered = [question for question in filtered if question.is_clarification]
        else:
            return _refuse_clinical_questions(state, refusal, filtered_ids)
    planned = plan_questions(
        candidate_question_ids=filtered_ids,
        signals=state.get("signals") if type(state.get("signals")) is dict else {},
        answered_question_ids=state.get("answeredQuestionIds", []),
    )
    question_ids = list(planned.question_ids)
    if not question_ids:
        return {"candidateQuestionIds": filtered_ids, "plannedQuestionIds": []}

    return {
        "candidateQuestionIds": filtered_ids,
        "plannedQuestionIds": question_ids,
        "questionRound": current_round + 1,
        "processedRequestIds": [*state.get("processedRequestIds", []), state["requestId"]],
        "processedMessageIds": [*state.get("processedMessageIds", []), state["messageId"]],
        "requiredAction": "ASK_CLARIFYING_QUESTIONS",
        "stopConversation": False,
    }


def _refuse_clinical_questions(
    state: Mapping[str, object], refusal: str | None, rejected_ids: list[str]
) -> dict[str, object]:
    """Stop with a stated limitation instead of asking an unrelated clinical question.

    Reached only when no clarification question is available either, so there is nothing useful
    left to ask. Says what is true — the complaint was understood, the ruleset cannot stratify it
    — which is neither "you are fine" nor "this is not our field".
    """

    rejected = [
        {"questionId": question_id, "reason": refusal or "CLINICAL_CATALOG_NOT_ALLOWED"}
        for question_id in rejected_ids
    ]
    return {
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "selectedCatalogType": "NONE",
        "rejectedQuestionIds": rejected,
        "requiredAction": "ROUTE_TO_HEALTHCARE_WORKER",
        "stopConversation": True,
        # Says what is true: the complaint was understood, the ruleset cannot stratify it. It is
        # explicitly not "you are fine" and not "this is not our field".
        "completionReason": CompletionReason.RULESET_COVERAGE_LIMITATION,
        "reasonCodes": ["RULESET_COVERAGE_LIMITATION"],
    }


def _concurrency_guard(state: Mapping[str, object]) -> dict[str, object] | None:
    if state.get("expectedStateVersion") == state.get("stateVersion"):
        return None
    errors = list(state.get("processingErrors", []))
    errors.append(
        {"code": "STALE_STATE_VERSION", "node": "question_planner", "message": "conflict"}
    )
    return {
        "triageOutcome": None,
        "requiredAction": "STATE_VERSION_CONFLICT",
        "stopConversation": True,
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "processingErrors": errors,
    }


def _is_duplicate(state: Mapping[str, object]) -> bool:
    return (
        state.get("requestId") in state.get("processedRequestIds", [])
        or state.get("messageId") in state.get("processedMessageIds", [])
    )


def _filter_context(state: Mapping[str, object]) -> FilterContext:
    missing = frozenset([*state.get("missingRequiredFields", []), *state.get("unknownFields", [])])
    return FilterContext(
        target_entity=_enum(state.get("targetEntity"), TargetEntity, TargetEntity.UNKNOWN),
        stage=_enum(state.get("stage"), CareStage, CareStage.UNKNOWN),
        intent=_enum(state.get("intent"), IntentType, IntentType.UNKNOWN),
        context_status=_enum(
            state.get("contextResolutionStatus"),
            ContextResolutionStatus,
            ContextResolutionStatus.INSUFFICIENT_CONTEXT,
        ),
        missing_fields=missing,
        missing_signals=missing,
        answered_question_ids=frozenset(state.get("answeredQuestionIds", [])),
        signals=state.get("signals") if type(state.get("signals")) is dict else {},
    )


def _enum(value: object, enum_type: type[_EnumT], default: _EnumT) -> _EnumT:
    if isinstance(value, enum_type):
        return value
    if type(value) is str:
        try:
            return enum_type(value)
        except ValueError:
            pass
    return default
