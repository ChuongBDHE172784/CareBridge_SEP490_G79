"""Typed state contract for the deterministic AI Triage graph.

This module deliberately contains no graph, node, model, retrieval, or transport logic.
The initializer creates a complete fail-safe state so later LangGraph nodes can return
partial updates without inheriting defaults from the legacy V1 graph.
"""

from __future__ import annotations

from math import isfinite
from typing import Literal, Mapping, Sequence, TypeAlias, TypedDict

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
    MatchedRuleTrace,
    PendingRiskStatus,
    ScopeStatus,
)
from app.triage.coverage_resolver import CoverageStatus
from app.triage.dataset_scope_nodes import SubjectScope

MAXIMUM_QUESTION_ROUNDS = 3

PossiblePregnancy = Literal["YES", "NO", "UNKNOWN", "CONFLICTED"]
TriageOutcome = Literal["RED", "YELLOW", "GREEN", "NEEDS_MORE_INFO", "OUT_OF_SCOPE"]
JsonValue: TypeAlias = (
    str | int | float | bool | None | list["JsonValue"] | dict[str, "JsonValue"]
)
RawMessage: TypeAlias = dict[str, JsonValue]
FinalResponse: TypeAlias = dict[str, JsonValue]


class ProcessingError(TypedDict):
    """A controlled internal error suitable for persistence and audit."""

    code: str
    node: str
    message: str


class MatchedRuleAudit(TypedDict):
    """JSON-shaped rule trace retained in the persisted graph state."""

    ruleId: str
    outcome: str
    priority: int
    ruleVersion: str
    role: str
    suppressionReason: str | None
    missingFields: list[str]


class TriageState(TypedDict):
    """Complete shared state for Phase 2's deterministic LangGraph."""

    sessionId: str
    stateVersion: int
    expectedStateVersion: int
    requestId: str
    messageId: str
    processedRequestIds: list[str]
    processedMessageIds: list[str]
    rawMessages: list[RawMessage]
    latestUserMessage: str
    targetEntity: TargetEntity
    targetEntitySource: ResolutionSource
    intent: IntentType
    intentSource: ResolutionSource
    confirmedConversationIntent: IntentType
    stage: CareStage
    contextResolutionStatus: ContextResolutionStatus
    contextConflicts: list[str]
    activeProfileId: str | None
    possiblePregnancy: PossiblePregnancy
    gestationalWeek: int | None
    postpartumDay: int | None
    babyAgeMonths: int | None
    signals: dict[str, JsonValue]
    measurements: dict[str, JsonValue]
    dataConflicts: list[str]
    answeredQuestionIds: list[str]
    askedQuestionIds: list[str]
    submittedOptionQuestionIds: list[str]
    submittedOptionCodes: list[str]
    unknownFields: list[str]
    safetyScreenStatus: DatasetStatus
    contextDatasetStatus: DatasetStatus
    greenEligibilityDatasetStatus: DatasetStatus
    scopeStatus: ScopeStatus
    #: Whether the person is someone CareBridge covers. Never CONFIRMED_OUT_OF_SCOPE.
    subjectScope: SubjectScope
    #: Whether the complaint just raised is something CareBridge assesses. Only this may be ruled
    #: out, so a pregnant user asking about wrist pain gets that complaint stopped without the
    #: pregnancy itself being declared outside scope.
    complaintScope: ScopeStatus
    #: What an OUT_OF_SCOPE verdict covers. CURRENT_COMPLAINT_ONLY unless stated otherwise.
    outcomeAppliesTo: str
    #: Whether the ruleset can assess what was reported â€” distinct from whether it is in scope.
    coverageStatus: CoverageStatus
    coverageReasonCodes: list[str]
    supportedSymptomCodes: list[str]
    unsupportedSymptomCodes: list[str]
    coverageLimitations: list[str]
    #: Set by the coverage resolver; the clinical catalogue is refused while true.
    blocksClinicalQuestionPlanner: bool
    blocksGreen: bool
    #: Which catalogue the turn actually used, and why questions were turned down.
    selectedCatalogType: str
    rejectedQuestionIds: list[dict[str, str]]
    pendingRiskStatuses: list[PendingRiskStatus]
    primaryPendingRiskStatus: PendingRiskStatus | None
    completionReason: CompletionReason | None
    missingRequiredFields: list[str]
    candidateQuestionIds: list[str]
    plannedQuestionIds: list[str]
    questionRound: int
    maximumQuestionRounds: int
    decisiveRuleIds: list[str]
    allMatchedRules: list[MatchedRuleAudit]
    triageOutcome: TriageOutcome | None
    requiredAction: str | None
    reasonCodes: list[str]
    stopConversation: bool
    rulesetVersion: str | None
    rulesetHash: str | None
    processingErrors: list[ProcessingError]
    finalResponse: FinalResponse | None
    citations: list[dict[str, JsonValue]]
    evidenceStatus: Literal["AVAILABLE", "PENDING", "UNAVAILABLE", "REJECTED"]
    evidenceRejections: list[str]
    rationale: str
    readingLinks: list[str]


def create_initial_state(
    *,
    session_id: str,
    state_version: int,
    expected_state_version: int | None = None,
    request_id: str,
    message_id: str,
    latest_user_message: str,
    raw_messages: Sequence[Mapping[str, JsonValue]] = (),
    active_profile_id: str | None = None,
) -> TriageState:
    """Create a complete V2 state without inferring any clinical fact.

    Validation of identifiers and version freshness belongs to ``input_validator`` in
    P2-T2/P2-T10. This factory only preserves caller input, owns defensive copies, and
    provides explicit unresolved defaults. In particular, it never creates an outcome,
    required action, response, or reading link.
    """

    return TriageState(
        sessionId=session_id,
        stateVersion=state_version,
        expectedStateVersion=(
            state_version if expected_state_version is None else expected_state_version
        ),
        requestId=request_id,
        messageId=message_id,
        processedRequestIds=[],
        processedMessageIds=[],
        rawMessages=[_copy_json_object(message) for message in raw_messages],
        latestUserMessage=latest_user_message,
        targetEntity=TargetEntity.UNKNOWN,
        targetEntitySource=ResolutionSource.NONE,
        intent=IntentType.UNKNOWN,
        intentSource=ResolutionSource.NONE,
        confirmedConversationIntent=IntentType.UNKNOWN,
        stage=CareStage.UNKNOWN,
        contextResolutionStatus=ContextResolutionStatus.INSUFFICIENT_CONTEXT,
        contextConflicts=[],
        activeProfileId=active_profile_id,
        possiblePregnancy="UNKNOWN",
        gestationalWeek=None,
        postpartumDay=None,
        babyAgeMonths=None,
        signals={},
        measurements={},
        dataConflicts=[],
        answeredQuestionIds=[],
        askedQuestionIds=[],
        submittedOptionQuestionIds=[],
        submittedOptionCodes=[],
        unknownFields=[],
        safetyScreenStatus=DatasetStatus.INCOMPLETE,
        contextDatasetStatus=DatasetStatus.INCOMPLETE,
        greenEligibilityDatasetStatus=DatasetStatus.INCOMPLETE,
        scopeStatus=ScopeStatus.UNKNOWN,
        subjectScope=SubjectScope.UNKNOWN,
        complaintScope=ScopeStatus.UNKNOWN,
        outcomeAppliesTo="CURRENT_COMPLAINT_ONLY",
        coverageStatus=CoverageStatus.UNKNOWN,
        coverageReasonCodes=[],
        supportedSymptomCodes=[],
        unsupportedSymptomCodes=[],
        coverageLimitations=[],
        blocksClinicalQuestionPlanner=False,
        blocksGreen=True,
        selectedCatalogType="NONE",
        rejectedQuestionIds=[],
        pendingRiskStatuses=[],
        primaryPendingRiskStatus=None,
        completionReason=None,
        missingRequiredFields=[],
        candidateQuestionIds=[],
        plannedQuestionIds=[],
        questionRound=0,
        maximumQuestionRounds=MAXIMUM_QUESTION_ROUNDS,
        decisiveRuleIds=[],
        allMatchedRules=[],
        triageOutcome=None,
        requiredAction=None,
        reasonCodes=[],
        stopConversation=False,
        rulesetVersion=None,
        rulesetHash=None,
        processingErrors=[],
        finalResponse=None,
        citations=[],
        evidenceStatus="PENDING",
        evidenceRejections=[],
        rationale="",
        readingLinks=[],
    )


def matched_rule_trace_to_audit(trace: MatchedRuleTrace) -> MatchedRuleAudit:
    """Convert the rule engine's dataclass trace to a persistence-safe state value."""

    return MatchedRuleAudit(
        ruleId=trace.rule_id,
        outcome=trace.outcome,
        priority=trace.priority,
        ruleVersion=trace.rule_version,
        role=trace.role,
        suppressionReason=trace.suppression_reason,
        missingFields=list(trace.missing_fields),
    )


def _copy_json_object(value: Mapping[str, JsonValue]) -> dict[str, JsonValue]:
    copied = _copy_json_value(value, ancestors=set())
    if not isinstance(copied, dict):  # Mapping input makes this defensive branch unreachable.
        raise TypeError("raw message must be a JSON object")
    return copied


def _copy_json_value(value: object, *, ancestors: set[int]) -> JsonValue:
    """Copy and canonicalize JSON data without accepting opaque Python objects."""

    if value is None or isinstance(value, (str, bool, int)):
        return value
    if isinstance(value, float):
        if not isfinite(value):
            raise ValueError("JSON numbers must be finite")
        return value

    if isinstance(value, Mapping):
        identity = id(value)
        if identity in ancestors:
            raise ValueError("cyclic JSON object")
        ancestors.add(identity)
        try:
            result: dict[str, JsonValue] = {}
            for key, item in value.items():
                if not isinstance(key, str):
                    raise TypeError("JSON object keys must be strings")
                result[key] = _copy_json_value(item, ancestors=ancestors)
            return result
        finally:
            ancestors.remove(identity)

    if isinstance(value, Sequence) and not isinstance(value, (str, bytes, bytearray)):
        identity = id(value)
        if identity in ancestors:
            raise ValueError("cyclic JSON array")
        ancestors.add(identity)
        try:
            return [_copy_json_value(item, ancestors=ancestors) for item in value]
        finally:
            ancestors.remove(identity)

    raise TypeError("value is not JSON-compatible")
