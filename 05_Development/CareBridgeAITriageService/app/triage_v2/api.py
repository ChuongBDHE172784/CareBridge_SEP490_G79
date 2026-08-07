"""Internal, fail-closed transport boundary for deterministic Triage V2 turns."""

from __future__ import annotations

import re
from copy import deepcopy
from math import isfinite
from time import monotonic
from typing import Any, Literal

from fastapi import Header, HTTPException
from pydantic import BaseModel, ConfigDict, Field, StringConstraints, field_validator
from typing_extensions import Annotated

from app.config import TRIAGE_V2_INTERNAL_API_KEY
from app.context import CareStage, ResolutionSource, TargetEntity
from app.gemini_client import get_gemini_client
from app.questions.catalog import CATALOG
from app.rules.registry import get_registry
from app.triage_v2.graph import build_triage_v2_graph, graph_config
from app.triage_v2.extraction import extract_and_validate
from app.triage_v2.global_safety_gate import global_safety_gate
from app.triage_v2.evidence_retrieval import retrieve_verified_evidence
from app.evidence_registry_client import approved_sources_for_stage
from app.triage_v2.state import TriageV2State, create_initial_state
from app.triage_v2.observability import metrics


SafeId = Annotated[str, StringConstraints(pattern=r"^[A-Za-z0-9_-]{16,64}$")]
_PRESENCE = {"PRESENT", "ABSENT", "UNKNOWN", "CONFLICTED", "UNAWARE_OR_UNMEASURABLE"}
_TEMPORAL = {"CURRENT", "HISTORICAL"}
#: Where a belief came from. Kept in lockstep with CanonicalAnswerMapper.Provenance in Java.
_PROVENANCE = {
    "USER_REPORTED", "QUESTION_ANSWER", "MEASURED", "LLM_EXTRACTED_VALIDATED",
    "PROFILE_CONTEXT", "HEALTH_MEMORY_CONTEXT",
}
_CONFLICT_STATUS = {"NONE", "CONFLICTED"}
_OBSERVATION_FIELDS = {
    "presence", "temporalStatus", "currentVsHistorical", "explicitNegation", "current",
    "historicalPresence", "provenance", "sourceQuestionId", "sourceOptionCode",
    "mappingRuleVersion", "conflictStatus",
}
_SAFE_CODE = re.compile(r"^[A-Za-z0-9_.-]{1,64}$")
_UNITS = {"C", "F", "MMHG", "BPM", "PERCENT", "WEEKS", "DAYS", "MONTHS", "KG", "CM"}
_MEASUREMENT_CODES = frozenset(
    field
    for question in CATALOG.values()
    if question.measurement
    for field in question.resolves_fields
)


def approved_domains(stage: str) -> set[str]:
    """Internal evidence-registry lookup; this is not Internet browsing."""

    return {source.domain for source in approved_sources_for_stage(stage)}


class TriageV2TurnRequest(BaseModel):
    """Closed Java-to-Python request; prior state is server-persisted, never app-authored."""

    model_config = ConfigDict(extra="forbid")

    sessionId: str = Field(pattern=r"^[0-9a-fA-F-]{36}$")
    stateVersion: int = Field(ge=0)
    expectedStateVersion: int = Field(ge=0)
    requestId: SafeId
    messageId: SafeId
    latestUserMessage: str = Field(min_length=1, max_length=2_000)
    activeProfileId: str | None = Field(default=None, max_length=64)
    selectedTarget: Literal["MOTHER", "BABY", "UNKNOWN"] = "UNKNOWN"
    journeyContext: dict[str, Any] = Field(default_factory=dict)
    previousState: dict[str, Any] | None = None
    signals: dict[str, Any] = Field(default_factory=dict)
    measurements: dict[str, Any] = Field(default_factory=dict)
    #: Questions the Java boundary resolved through the canonical answer mapper this turn. Only
    #: identifiers travel here; the signals they imply were already derived server-side.
    answeredQuestionIds: list[str] = Field(default_factory=list, max_length=8)
    expectedRulesetHash: str = Field(min_length=64, max_length=64)

    @field_validator("answeredQuestionIds")
    @classmethod
    def validate_answered_question_ids(cls, value: list[str]) -> list[str]:
        unknown = [item for item in value if item not in CATALOG]
        if unknown:
            raise ValueError("invalid answered question id")
        return value

    @field_validator("signals")
    @classmethod
    def validate_signals(cls, value: dict[str, Any]) -> dict[str, Any]:
        return _safe_signals(value)

    @field_validator("measurements")
    @classmethod
    def validate_measurements(cls, value: dict[str, Any]) -> dict[str, Any]:
        return _safe_measurements(value)

    @field_validator("journeyContext")
    @classmethod
    def validate_journey_context(cls, value: dict[str, Any]) -> dict[str, Any]:
        return _safe_journey_context(value)


class TriageV2TurnResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    state: dict[str, Any]
    readiness: str
    rulesetVersion: str
    rulesetHash: str


def require_internal_key(x_carebridge_internal_key: str | None = Header(default=None)) -> None:
    """Keep V2 unreachable unless both services share an explicitly configured secret."""

    if not TRIAGE_V2_INTERNAL_API_KEY:
        raise HTTPException(status_code=503, detail="Triage V2 internal API is disabled")
    if x_carebridge_internal_key != TRIAGE_V2_INTERNAL_API_KEY:
        raise HTTPException(status_code=403, detail="Forbidden")


def execute_turn(request: TriageV2TurnRequest) -> TriageV2TurnResponse:
    started = monotonic()
    registry = get_registry()
    if request.expectedRulesetHash != registry.ruleset_sha256:
        metrics.record_error("HASH_MISMATCH")
        raise HTTPException(status_code=409, detail="Triage V2 ruleset hash mismatch")

    state = _turn_state(request)
    # RED must not wait for Gemini. This is a pre-check only; the graph runs the same gate
    # again at its entry and remains the workflow authority.
    state.update(global_safety_gate(state))
    gemini = None if state.get("triageOutcome") == "RED" else get_gemini_client()
    extraction = extract_and_validate(request.latestUserMessage, gemini) if gemini else None
    if gemini is not None and extraction is None:
        metrics.record_error("EXTRACTION_REJECTED")
    if extraction is not None:
        # New grounded observations must never be hidden by an older value for the
        # same signal. The extractor describes the latest message, so its current
        # observation supersedes the prior current value before global safety runs.
        state["signals"] = {**_mapping(state.get("signals")), **extraction.signals}
    try:
        # A fresh graph instance makes Java's persisted state authoritative and avoids making
        # process-local LangGraph checkpoints a recovery dependency.
        result = build_triage_v2_graph().invoke(state, config=graph_config(request.sessionId))
    except (TypeError, ValueError) as failure:
        metrics.record_error("INVALID_STATE")
        raise HTTPException(status_code=422, detail="Invalid Triage V2 state") from failure
    completed_state = dict(result)
    # Retrieval is strictly post-outcome. RED never waits on the registry/RAG path; its action
    # is already rendered and citations remain optional.
    try:
        evidence_domains = set() if completed_state.get("triageOutcome") == "RED" else (
            approved_domains(str(getattr(completed_state.get("stage"), "value",
                                         completed_state.get("stage", "UNKNOWN"))))
            if completed_state.get("triageOutcome") == "YELLOW" else set()
        )
        completed_state["citations"] = retrieve_verified_evidence(
            completed_state, allowed_domains=evidence_domains, on_reject=metrics.record_error
        )
    except Exception:  # Evidence is optional and must not change a completed disposition.
        metrics.record_error("CITATION_REJECTED")
        completed_state["citations"] = []
    metrics.record_turn(completed_state, (monotonic() - started) * 1_000)
    return TriageV2TurnResponse(
        state=completed_state,
        readiness="READY",
        rulesetVersion=registry.ruleset_version,
        rulesetHash=registry.ruleset_sha256,
    )


def _turn_state(request: TriageV2TurnRequest) -> TriageV2State:
    if request.previousState is None:
        state = create_initial_state(
            session_id=request.sessionId,
            state_version=request.stateVersion,
            expected_state_version=request.expectedStateVersion,
            request_id=request.requestId,
            message_id=request.messageId,
            latest_user_message=request.latestUserMessage,
            raw_messages=({"role": "USER", "content": request.latestUserMessage,
                           "messageId": request.messageId},),
            active_profile_id=request.activeProfileId,
        )
        if request.selectedTarget in {"MOTHER", "BABY"}:
            state["targetEntity"] = TargetEntity(request.selectedTarget)
            state["targetEntitySource"] = ResolutionSource.EXPLICIT_SELECTED_PROFILE
        journey = request.journeyContext
        if "stage" in journey:
            state["stage"] = CareStage(journey["stage"])
        for field in ("possiblePregnancy", "gestationalWeek", "postpartumDay", "babyAgeMonths"):
            if field in journey:
                state[field] = journey[field]
    else:
        state = deepcopy(request.previousState)
        if state.get("sessionId") != request.sessionId or state.get("stateVersion") != request.stateVersion:
            raise HTTPException(status_code=409, detail="Persisted Triage V2 state conflict")
        state.update(
            requestId=request.requestId,
            messageId=request.messageId,
            latestUserMessage=request.latestUserMessage,
            expectedStateVersion=request.expectedStateVersion,
            finalResponse=None,
        )
        messages = list(state.get("rawMessages", []))
        messages.append({"role": "USER", "content": request.latestUserMessage,
                         "messageId": request.messageId})
        state["rawMessages"] = messages

    # These are structured observations produced by the trusted Java boundary/extractor;
    # global safety still evaluates them independently before routing.
    state["signals"] = _merge_observations(state.get("signals"), request.signals)
    state["measurements"] = {**_mapping(state.get("measurements")), **request.measurements}
    # Record what the user has already answered so the planner stops re-asking it. This holds
    # even when the mapping contract could not yet interpret the chosen option: the question was
    # genuinely answered, and re-asking it forever is its own failure mode.
    answered = list(state.get("answeredQuestionIds", []))
    for question_id in request.answeredQuestionIds:
        if question_id not in answered:
            answered.append(question_id)
    state["answeredQuestionIds"] = answered
    return state  # type: ignore[return-value]


def _mapping(value: object) -> dict[str, Any]:
    return dict(value) if type(value) is dict else {}


def _merge_observations(current: object, delta: dict[str, Any]) -> dict[str, Any]:
    merged = _mapping(current)
    for code, observation in delta.items():
        if code not in merged:
            merged[code] = observation
            continue
        prior = merged[code] if type(merged[code]) is list else [merged[code]]
        incoming = observation if type(observation) is list else [observation]
        merged[code] = [*prior, *incoming][-4:]
    return merged


def _safe_signals(value: dict[str, Any]) -> dict[str, Any]:
    if len(value) > 50:
        raise ValueError("too many signals")
    return {code: _safe_signal_value(code, observation) for code, observation in value.items()}


def _safe_signal_value(code: str, value: Any) -> Any:
    if code not in get_registry().signal_display_text:
        raise ValueError("invalid signal code")
    if type(value) is str:
        if value not in _PRESENCE:
            raise ValueError("invalid signal presence")
        return value
    if type(value) is list:
        if not 1 <= len(value) <= 4:
            raise ValueError("invalid signal observation count")
        return [_safe_signal_value(code, item) for item in value]
    if type(value) is not dict or set(value) - _OBSERVATION_FIELDS:
        raise ValueError("invalid signal observation")
    if value.get("presence") not in _PRESENCE:
        raise ValueError("invalid signal presence")
    for field in ("temporalStatus", "currentVsHistorical"):
        if field in value and value[field] not in _TEMPORAL:
            raise ValueError("invalid signal temporal status")
    if "historicalPresence" in value and value["historicalPresence"] not in _PRESENCE:
        raise ValueError("invalid historical signal presence")
    for field in ("explicitNegation", "current"):
        if field in value and type(value[field]) is not bool:
            raise ValueError("invalid signal boolean")
    if "provenance" in value and value["provenance"] not in _PROVENANCE:
        raise ValueError("invalid signal provenance")
    if "conflictStatus" in value and value["conflictStatus"] not in _CONFLICT_STATUS:
        raise ValueError("invalid signal conflict status")
    for field in ("sourceQuestionId", "sourceOptionCode", "mappingRuleVersion"):
        if field in value and (type(value[field]) is not str
                               or _SAFE_CODE.fullmatch(value[field]) is None):
            raise ValueError("invalid signal provenance reference")
    return dict(value)


def _safe_measurements(value: dict[str, Any]) -> dict[str, Any]:
    if len(value) > 50:
        raise ValueError("too many measurements")
    result: dict[str, Any] = {}
    for code, measurement in value.items():
        if code not in _MEASUREMENT_CODES or type(measurement) is bool:
            raise ValueError("invalid measurement")
        if type(measurement) in {int, float}:
            if not isfinite(measurement) or abs(measurement) > 1_000_000:
                raise ValueError("invalid measurement value")
            result[code] = measurement
            continue
        if type(measurement) is not dict or set(measurement) - {
            "value", "unit", "status", "temporalStatus"
        }:
            raise ValueError("invalid measurement shape")
        number = measurement.get("value")
        if number is not None and (type(number) not in {int, float}
                                   or not isfinite(number) or abs(number) > 1_000_000):
            raise ValueError("invalid measurement value")
        if "unit" in measurement and measurement["unit"] not in _UNITS:
            raise ValueError("invalid measurement unit")
        if "status" in measurement and measurement["status"] not in {
            "UNKNOWN", "UNAWARE_OR_UNMEASURABLE"
        }:
            raise ValueError("invalid measurement status")
        if "temporalStatus" in measurement and measurement["temporalStatus"] not in _TEMPORAL:
            raise ValueError("invalid measurement temporal status")
        result[code] = dict(measurement)
    return result


def _safe_journey_context(value: dict[str, Any]) -> dict[str, Any]:
    allowed = {"stage", "possiblePregnancy", "gestationalWeek", "postpartumDay", "babyAgeMonths"}
    if set(value) - allowed:
        raise ValueError("invalid journey context field")
    result: dict[str, Any] = {}
    if "stage" in value:
        stage = value["stage"]
        if stage not in {item.value for item in CareStage if item is not CareStage.CONFLICTED}:
            raise ValueError("invalid journey stage")
        result["stage"] = stage
    if "possiblePregnancy" in value:
        possible = value["possiblePregnancy"]
        if possible not in {"YES", "NO", "UNKNOWN", "CONFLICTED"}:
            raise ValueError("invalid possible pregnancy value")
        result["possiblePregnancy"] = possible
    for field in ("gestationalWeek", "postpartumDay", "babyAgeMonths"):
        if field in value:
            number = value[field]
            if type(number) is not int or not 0 <= number <= 1_000_000:
                raise ValueError("invalid journey context number")
            result[field] = number
    return result
