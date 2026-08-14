"""Internal, fail-closed transport boundary for deterministic triage turns."""

from __future__ import annotations

import re
from copy import deepcopy
from math import isfinite
from time import monotonic
from typing import Any, Literal

from fastapi import Header, HTTPException
from pydantic import (
    BaseModel,
    ConfigDict,
    Field,
    StringConstraints,
    field_validator,
    model_validator,
)
from typing_extensions import Annotated

from app.config import PYTHON_SERVICE_TIMEOUT_SECONDS, TRIAGE_INTERNAL_API_KEY
from app.context import CareStage, IntentType, ResolutionSource, TargetEntity
from app.gemini_client import get_gemini_client
from app.questions.catalog import CATALOG
from app.rules.registry import get_registry
from app.triage.graph import build_triage_graph, graph_config
from app.triage.extraction import extract_and_validate
from app.triage.deterministic_signals import detect_danger_signals, merge_as_floor
from app.triage.deterministic_measurements import (
    ReportedMeasurements,
    extract_reported_measurements,
)
from app.triage.global_safety_gate import global_safety_gate
from app.triage.evidence_retrieval import retrieve_verified_evidence
from app.triage.evidence_finalizer import finalize_evidence
from app.evidence_registry_client import (
    approved_sources_for_stage,
    cached_approved_sources_for_stage,
)
from app.triage.state import TriageState, create_initial_state
from app.triage.observability import metrics


SafeId = Annotated[str, StringConstraints(pattern=r"^[A-Za-z0-9_-]{16,64}$")]
_PRESENCE = {"PRESENT", "ABSENT", "UNKNOWN", "CONFLICTED", "UNAWARE_OR_UNMEASURABLE"}
_TEMPORAL = {"CURRENT", "HISTORICAL"}
#: Where a belief came from. Kept in lockstep with CanonicalAnswerMapper.Provenance in Java.
_PROVENANCE = {
    "USER_REPORTED", "QUESTION_ANSWER", "MEASURED", "LLM_EXTRACTED_VALIDATED",
    "PROFILE_CONTEXT", "HEALTH_MEMORY_CONTEXT", "USER_REPORTED_TEXT",
}
_CONFLICT_STATUS = {"NONE", "CONFLICTED"}
_OBSERVATION_FIELDS = {
    "presence", "temporalStatus", "currentVsHistorical", "explicitNegation", "current",
    "historicalPresence", "provenance", "sourceQuestionId", "sourceOptionCode",
    "mappingRuleVersion", "conflictStatus",
}
_SAFE_CODE = re.compile(r"^[A-Za-z0-9_.-]{1,64}$")
_UNITS = {"C", "F", "MMHG", "BPM", "PERCENT", "WEEKS", "DAYS", "MONTHS", "KG", "CM"}
_MEASUREMENT_CODES = frozenset({"temperatureC", "babyAgeMonths"}) | frozenset(
    field
    for question in CATALOG.values()
    if question.measurement
    for field in question.resolves_fields
)


def approved_domains(stage: str) -> set[str]:
    """Internal evidence-registry lookup; this is not Internet browsing."""

    return {source.domain for source in approved_sources_for_stage(stage)}


class TriageTurnRequest(BaseModel):
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
    submittedOptionQuestionIds: list[str] = Field(default_factory=list, max_length=8)
    submittedOptionCodes: list[str] = Field(default_factory=list, max_length=8)
    expectedRulesetHash: str = Field(min_length=64, max_length=64)

    @field_validator("answeredQuestionIds")
    @classmethod
    def validate_answered_question_ids(cls, value: list[str]) -> list[str]:
        unknown = [item for item in value if item not in CATALOG]
        if unknown:
            raise ValueError("invalid answered question id")
        return value

    @field_validator("submittedOptionCodes")
    @classmethod
    def validate_submitted_option_codes(cls, value: list[str]) -> list[str]:
        fixed = {option.option_code for question in CATALOG.values() for option in question.options}
        if any(item not in fixed for item in value):
            raise ValueError("invalid submitted option code")
        return value

    @model_validator(mode="after")
    def validate_answer_option_pairs(self) -> "TriageTurnRequest":
        if not self.submittedOptionQuestionIds and self.submittedOptionCodes:
            # Compatibility for callers predating the explicit option-question ledger. It is
            # safe only when every answered question owns exactly one submitted option.
            if len(self.answeredQuestionIds) == len(self.submittedOptionCodes):
                self.submittedOptionQuestionIds = list(self.answeredQuestionIds)
        if len(self.submittedOptionQuestionIds) != len(self.submittedOptionCodes):
            raise ValueError("submitted option questions and codes must be paired")
        if not set(self.submittedOptionQuestionIds) <= set(self.answeredQuestionIds):
            raise ValueError("submitted option question must be answered")
        for question_id, option_code in zip(
            self.submittedOptionQuestionIds, self.submittedOptionCodes
        ):
            allowed = {option.option_code for option in CATALOG[question_id].options}
            if option_code not in allowed:
                raise ValueError("submitted option does not belong to answered question")
        return self

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


class TriageTurnResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    state: dict[str, Any]
    readiness: str
    rulesetVersion: str
    rulesetHash: str


def require_internal_key(x_carebridge_internal_key: str | None = Header(default=None)) -> None:
    """Keep triage unreachable unless both services share an explicitly configured secret."""

    if not TRIAGE_INTERNAL_API_KEY:
        raise HTTPException(status_code=503, detail="Triage internal API is disabled")
    if x_carebridge_internal_key != TRIAGE_INTERNAL_API_KEY:
        raise HTTPException(status_code=403, detail="Forbidden")


def execute_turn(request: TriageTurnRequest) -> TriageTurnResponse:
    started = monotonic()
    turn_deadline = started + PYTHON_SERVICE_TIMEOUT_SECONDS
    extraction_deadline = turn_deadline - 1.0
    registry = get_registry()
    if request.expectedRulesetHash != registry.ruleset_sha256:
        metrics.record_error("HASH_MISMATCH")
        raise HTTPException(status_code=409, detail="Triage ruleset hash mismatch")

    state = _turn_state(request)
    reported_measurements = extract_reported_measurements(
        request.latestUserMessage, target_entity=state.get("targetEntity")
    )
    if reported_measurements.invalid_codes:
        raise HTTPException(
            status_code=422,
            detail={
                "code": "IMPLAUSIBLE_MEASUREMENT",
                "fields": sorted(reported_measurements.invalid_codes),
            },
        )
    _merge_reported_measurements(
        state,
        reported_measurements,
    )
    # A danger phrase in the message becomes a signal before anything else runs, so the gate
    # below sees it whether or not Gemini is reachable. Placed under the caller's signals, not
    # over them: see merge_as_floor.
    # The stage-scoped groups (D-032) may only read a stage the server already holds. At this
    # point that is the only kind there is: `stage` comes from Java's journeyContext or from the
    # persisted previous state, both server-side, and extraction — the one path that could infer
    # a stage — runs below this line and is barred from writing stage at all.
    state["signals"] = merge_as_floor(
        _mapping(state.get("signals")),
        detect_danger_signals(
            request.latestUserMessage,
            stage=state.get("stage"),
            stage_source="EXPLICIT_SELECTED_PROFILE",
        ),
    )
    # RED must not wait for Gemini. This is a pre-check only; the graph runs the same gate
    # again at its entry and remains the workflow authority.
    state.update(global_safety_gate(state))
    gemini = None if state.get("triageOutcome") == "RED" else get_gemini_client()
    extraction = extract_and_validate(
        request.latestUserMessage, gemini, deadline=extraction_deadline
    ) if gemini else None
    if gemini is not None and extraction is None:
        metrics.record_error("EXTRACTION_REJECTED")
    if extraction is not None:
        state["signals"] = _merge_observations(state.get("signals"), extraction.signals)
    try:
        # A fresh graph instance makes Java's persisted state authoritative and avoids making
        # process-local LangGraph checkpoints a recovery dependency.
        result = build_triage_graph().invoke(state, config=graph_config(request.sessionId))
    except (TypeError, ValueError) as failure:
        metrics.record_error("INVALID_STATE")
        raise HTTPException(status_code=422, detail="Invalid triage state") from failure
    completed_state = dict(result)
    # LangGraph's interrupt marker is process-local control metadata, not workflow state. Java
    # persists the explicit plannedQuestionIds ledger and must never receive this private key.
    completed_state.pop("__interrupt__", None)
    # Submitted option codes are trusted request metadata for resolving this turn only. They are
    # deliberately excluded from the workflow response so Java cannot persist and replay a stale
    # clarification answer on a later turn.
    completed_state.pop("submittedOptionCodes", None)
    # Retrieval is strictly post-outcome. RED may use a previously successful registry cache,
    # but never starts network I/O; without that cache its evidence remains honestly PENDING.
    retrieval_attempted = False
    rejections: list[str] = []
    try:
        outcome = completed_state.get("triageOutcome")
        stage = str(getattr(completed_state.get("stage"), "value",
                            completed_state.get("stage", "UNKNOWN")))
        if outcome == "RED":
            evidence_domains = {
                source.domain for source in cached_approved_sources_for_stage(stage)
            }
        elif outcome == "YELLOW" and monotonic() < extraction_deadline:
            retrieval_attempted = True
            evidence_domains = approved_domains(stage)
        else:
            # A terminal YELLOW turn has no later enrichment path. If its evidence budget was
            # already exhausted, report UNAVAILABLE rather than a PENDING state that can never
            # complete. RED remains PENDING when there is no warm cache, per the urgent path.
            retrieval_attempted = outcome == "YELLOW"
            evidence_domains = set()
        if evidence_domains:
            retrieval_attempted = True
            completed_state["citations"] = retrieve_verified_evidence(
                completed_state,
                allowed_domains=evidence_domains,
                on_reject=lambda code: (rejections.append(code), metrics.record_error(code)),
            )
        else:
            completed_state["citations"] = []
    except Exception:  # Evidence is optional and must not change a completed disposition.
        metrics.record_error("CITATION_REJECTED")
        rejections.append("CITATION_REJECTED")
        completed_state["citations"] = []
    completed_state["evidenceRejections"] = rejections
    completed_state = finalize_evidence(
        completed_state, retrieval_attempted=retrieval_attempted
    )
    metrics.record_turn(completed_state, (monotonic() - started) * 1_000)
    return TriageTurnResponse(
        state=completed_state,
        readiness="READY",
        rulesetVersion=registry.ruleset_version,
        rulesetHash=registry.ruleset_sha256,
    )


def _turn_state(request: TriageTurnRequest) -> TriageState:
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
        state.setdefault("askedQuestionIds", [])
        state.setdefault("confirmedConversationIntent", IntentType.UNKNOWN)
        state.setdefault("submittedOptionQuestionIds", [])
        state.setdefault("evidenceStatus", "PENDING")
        state.setdefault("evidenceRejections", [])
        state.setdefault("rationale", "")
        if state.get("sessionId") != request.sessionId or state.get("stateVersion") != request.stateVersion:
            raise HTTPException(status_code=409, detail="Persisted triage state conflict")
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
    state["submittedOptionCodes"] = list(request.submittedOptionCodes)
    state["submittedOptionQuestionIds"] = list(request.submittedOptionQuestionIds)
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


def _merge_reported_measurements(
    state: dict[str, Any], reported: ReportedMeasurements
) -> None:
    """Merge latest text below trusted structured measurements, never above them."""

    measurements = _mapping(state.get("measurements"))
    for code in reported.conflicted_codes:
        if not _is_structured_measurement(measurements.get(code)):
            # Ambiguous latest text must not silently retain an older text-derived value.
            measurements.pop(code, None)
    for code, observation in reported.measurements.items():
        if _is_structured_measurement(measurements.get(code)):
            continue
        measurements[code] = dict(observation)
    state["measurements"] = measurements

    structured_age = measurements.get("babyAgeMonths")
    if _is_structured_measurement(structured_age):
        value = (
            structured_age.get("value")
            if type(structured_age) is dict
            else structured_age
        )
        if type(value) is int or (type(value) is float and value.is_integer()):
            state["babyAgeMonths"] = int(value)
    elif reported.baby_age_months is not None:
        # Latest explicit text outranks stale journey context, matching stage resolution.
        state["babyAgeMonths"] = reported.baby_age_months


def _is_structured_measurement(value: object) -> bool:
    if type(value) in {int, float}:
        return True
    if type(value) is not dict or value.get("provenance") != "MEASURED":
        return False
    numeric_value = value.get("value")
    return (
        type(numeric_value) in {int, float}
        and value.get("temporalStatus") in {None, "CURRENT"}
        and value.get("status") in {None, "PRESENT"}
    )


def _merge_observations(current: object, delta: dict[str, Any]) -> dict[str, Any]:
    """Accumulate evidence so contradictions become ``CONFLICTED`` downstream.

    No provenance wins merely because it arrived last. In particular, an
    ``LLM_EXTRACTED_VALIDATED`` observation never overwrites a canonical ``QUESTION_ANSWER``;
    PRESENT versus ABSENT remains visible to the normalizer and follows the explicit
    ``CONFLICTED`` policy.
    """

    merged = _mapping(current)
    for code, observation in delta.items():
        if code not in merged:
            merged[code] = observation
            continue
        prior = merged[code] if type(merged[code]) is list else [merged[code]]
        incoming = observation if type(observation) is list else [observation]
        combined = [*prior, *incoming]
        question_answers = [
            item for item in combined
            if type(item) is dict and item.get("provenance") == "QUESTION_ANSWER"
        ]
        other_observations = [item for item in combined if item not in question_answers]
        # The transport schema caps one signal at four observations. Pin canonical question
        # answers first so repeated LLM extractions can only evict older non-canonical evidence.
        pinned = question_answers[-4:]
        remaining = 4 - len(pinned)
        merged[code] = [*pinned, *other_observations[-remaining:]] if remaining else pinned
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
            "value", "unit", "status", "temporalStatus", "provenance"
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
        if "provenance" in measurement and measurement["provenance"] not in _PROVENANCE:
            raise ValueError("invalid measurement provenance")
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
