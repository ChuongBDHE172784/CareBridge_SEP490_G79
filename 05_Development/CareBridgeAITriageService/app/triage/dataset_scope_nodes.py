"""Zero-trust dataset and positive-evidence scope nodes for triage."""

from __future__ import annotations

from enum import Enum
from typing import Any, Mapping

from app.context import CareStage, ContextResolutionStatus
from app.context.complaint_taxonomy import classify_complaint, may_return_out_of_scope
from app.rules.condition import Presence, signal_presence
from app.rules.evaluator import (
    DatasetStatus,
    PendingRiskStatus,
    ScopeStatus,
    _calc_context_dataset_status,
    _calc_safety_screen_status,
    _reproductive_evidence,
)
from app.rules.registry import load_dataset_requirements
from app.triage.signal_normalizer import normalize_signal_observation


class SubjectScope(str, Enum):
    """Whether the person is someone CareBridge covers.

    Deliberately has no CONFIRMED_OUT_OF_SCOPE member. A mother in a maternal stage is in scope,
    full stop; only an individual complaint can be ruled out. Making that unrepresentable is
    cheaper than remembering the rule.
    """

    IN_SCOPE = "IN_SCOPE"
    UNKNOWN = "UNKNOWN"
    CONFLICTED = "CONFLICTED"


def dataset_calculator(state: Mapping[str, object]) -> dict[str, object]:
    """Derive dataset status from state; caller completeness claims are not inputs."""

    signals = _trusted_signals(state.get("signals"))
    requirements = load_dataset_requirements()
    stage = _stage_value(state.get("stage"))
    context = _context(state)
    safety = _calc_safety_screen_status(signals, requirements)
    context_status = _context_status(stage, context, requirements)
    green = _green_status(stage, safety, context_status, context, requirements)
    pending = _pending(state, safety, context_status)
    missing = _missing_fields(stage, signals, context, requirements)
    return {
        "safetyScreenStatus": DatasetStatus(safety),
        "contextDatasetStatus": DatasetStatus(context_status),
        "greenEligibilityDatasetStatus": DatasetStatus(green),
        "pendingRiskStatuses": pending,
        "primaryPendingRiskStatus": _primary(pending),
        "missingRequiredFields": missing,
    }


def scope_calculator(state: Mapping[str, object]) -> dict[str, object]:
    """Decide scope at two levels: the person, and the complaint they just raised.

    These were one value, and collapsing them had a concrete cost. Being pregnant counted as
    reproductive evidence, out-of-scope required the absence of reproductive evidence, so a
    pregnant user could never have any complaint ruled out â€” wrist pain after the gym still
    entered obstetric questioning. Splitting them lets the wrist pain stop without ever implying
    the pregnancy itself is outside CareBridge.

    Subject scope answers "is this person someone CareBridge covers". For a mother in a maternal
    stage that is IN_SCOPE and stays IN_SCOPE. Complaint scope answers "is the thing they just
    described something CareBridge assesses", and only that one may be CONFIRMED_OUT_OF_SCOPE.
    """

    if state.get("triageOutcome") == "RED":
        return {"stopConversation": True, "candidateQuestionIds": [], "plannedQuestionIds": []}

    signals = _trusted_signals(state.get("signals"))
    classification = classify_complaint(
        state.get("latestUserMessage") if type(state.get("latestUserMessage")) is str else None,
        signals,
    )
    subject_reproductive = _has_reproductive_evidence(state, signals)
    # Complaint-level evidence is what the person actually reported, not the journey they are on.
    # A reported reproductive signal keeps the complaint in scope; a known pregnancy alone does
    # not. An UNRESOLVED pregnancy does, though: while we do not know whether she is pregnant, an
    # unexplained complaint might be the pregnancy, and ruling it out would be guessing.
    complaint_reproductive = (
        bool(_reproductive_evidence(signals, _context(state)))
        or state.get("possiblePregnancy") in {"YES", "UNKNOWN", "CONFLICTED"}
    )
    safety_complete = _status_value(state.get("safetyScreenStatus")) == DatasetStatus.COMPLETE.value
    context_conflicted = (
        state.get("contextResolutionStatus") == ContextResolutionStatus.CONFLICTED
        or state.get("contextResolutionStatus") == ContextResolutionStatus.CONFLICTED.value
    )

    subject_scope = (
        SubjectScope.CONFLICTED if context_conflicted
        else SubjectScope.IN_SCOPE if subject_reproductive
        else SubjectScope.UNKNOWN
    )

    if context_conflicted:
        return {
            "scopeStatus": ScopeStatus.CONFLICTED,
            "subjectScope": subject_scope,
            "complaintScope": ScopeStatus.CONFLICTED,
        }
    if may_return_out_of_scope(
        classification=classification,
        safety_screen_complete=safety_complete,
        has_reproductive_evidence=complaint_reproductive,
    ):
        return {
            # scopeStatus stays the complaint-level value so existing rules and audits keep
            # reading what they always read.
            "scopeStatus": ScopeStatus.CONFIRMED_OUT_OF_SCOPE,
            "subjectScope": subject_scope,
            "complaintScope": ScopeStatus.CONFIRMED_OUT_OF_SCOPE,
            # The verdict covers this complaint only. Nothing here says the person, the pregnancy
            # or the rest of the session is outside CareBridge.
            "outcomeAppliesTo": "CURRENT_COMPLAINT_ONLY",
            "triageOutcome": "OUT_OF_SCOPE",
            "requiredAction": "ROUTE_OUT_OF_SCOPE",
            "reasonCodes": ["CONFIRMED_NON_REPRODUCTIVE_COMPLAINT"],
            "stopConversation": True,
            "candidateQuestionIds": [],
            "plannedQuestionIds": [],
            "finalResponse": None,
            "readingLinks": [],
        }
    if complaint_reproductive or subject_reproductive:
        return {
            "scopeStatus": ScopeStatus.IN_SCOPE,
            "subjectScope": subject_scope,
            "complaintScope": (ScopeStatus.IN_SCOPE if complaint_reproductive
                               else ScopeStatus.UNKNOWN),
        }
    if classification.is_confirmed_non_reproductive:
        return {
            "scopeStatus": ScopeStatus.POSSIBLY_IN_SCOPE,
            "subjectScope": subject_scope,
            "complaintScope": ScopeStatus.POSSIBLY_IN_SCOPE,
        }
    return {
        "scopeStatus": ScopeStatus.UNKNOWN,
        "subjectScope": subject_scope,
        "complaintScope": ScopeStatus.UNKNOWN,
    }


def _context(state: Mapping[str, object]) -> dict[str, object]:
    return {
        "stage": _stage_value(state.get("stage")),
        "gestational_week": state.get("gestationalWeek"),
        "postpartum_day": state.get("postpartumDay"),
        "baby_age_months": state.get("babyAgeMonths"),
        "possible_pregnancy": state.get("possiblePregnancy"),
        **(state.get("measurements") if type(state.get("measurements")) is dict else {}),
    }


def _stage_value(value: object) -> str:
    raw = value.value if isinstance(value, CareStage) else value
    return raw if type(raw) is str else "UNKNOWN"


def _context_status(stage: str, context: Mapping[str, object], requirements: Mapping[str, Any]) -> str:
    if stage in {CareStage.INFANT_0_12M.value, CareStage.TODDLER_12_24M.value}:
        # Age alone still decides whether the paediatric context is usable: the remaining
        # paediatric fields drive question planning, not stage resolution, and requiring them
        # here would leave the context permanently incomplete before anything is asked.
        return (
            DatasetStatus.COMPLETE.value
            if context.get("baby_age_months") is not None
            else DatasetStatus.INCOMPLETE.value
        )
    return _calc_context_dataset_status(stage, context, requirements)


def _green_status(
    stage: str,
    safety: str,
    context_status: str,
    context: Mapping[str, object],
    requirements: Mapping[str, Any],
) -> str:
    if safety == DatasetStatus.CONFLICTED.value or context_status == DatasetStatus.CONFLICTED.value:
        return DatasetStatus.CONFLICTED.value
    if safety != DatasetStatus.COMPLETE.value or context_status != DatasetStatus.COMPLETE.value:
        return DatasetStatus.INCOMPLETE.value
    # The current canonical clinical registry has no baby-stage rule coverage.
    if stage in {CareStage.INFANT_0_12M.value, CareStage.TODDLER_12_24M.value}:
        return DatasetStatus.INCOMPLETE.value
    required = requirements.get("byStage", {}).get(stage, {}).get("greenEligibilityFields", [])
    for field in required:
        if field != "stage" and _is_unresolved(context.get(field)):
            return DatasetStatus.INCOMPLETE.value
    return DatasetStatus.COMPLETE.value


def _pending(state: Mapping[str, object], safety: str, context_status: str) -> list[PendingRiskStatus]:
    retained = [
        item
        for item in state.get("pendingRiskStatuses", [])
        if _status_value(item)
        not in {
            PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN.value,
            PendingRiskStatus.UNRESOLVED_CONTEXT.value,
        }
    ]
    if safety != DatasetStatus.COMPLETE.value:
        retained.append(PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN)
    if context_status != DatasetStatus.COMPLETE.value:
        retained.append(PendingRiskStatus.UNRESOLVED_CONTEXT)
    return list(dict.fromkeys(retained))


def _primary(items: list[object]) -> PendingRiskStatus | None:
    values = {_status_value(item) for item in items}
    for item in PendingRiskStatus:
        if item.value in values:
            return item
    return None


def _missing_fields(
    stage: str,
    signals: Mapping[str, object],
    context: Mapping[str, object],
    requirements: Mapping[str, Any],
) -> list[str]:
    missing = [
        code
        for code in requirements.get("globalSafety", {}).get("requiredSignalCodes", [])
        if signal_presence(signals, code)
        in {Presence.UNKNOWN, Presence.CONFLICTED, Presence.UNAWARE_OR_UNMEASURABLE}
    ]
    # Paediatric stages used to be hardcoded to baby_age_months because the requirements file had
    # no entry for them. It does now, so read it like every other stage â€” otherwise the paediatric
    # follow-up questions never count as missing and a baby complaint is never asked anything.
    required_context = requirements.get("byStage", {}).get(stage, {}).get("contextFields", [])
    missing.extend(
        field
        for field in required_context
        if field != "stage" and _is_unresolved(context.get(field))
    )
    return list(dict.fromkeys(missing))


def _has_reproductive_evidence(state: Mapping[str, object], signals: Mapping[str, object]) -> bool:
    if state.get("possiblePregnancy") in {"YES", "UNKNOWN", "CONFLICTED"}:
        return True
    stage = _stage_value(state.get("stage"))
    if stage in {"POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM_MOTHER"}:
        return True
    return bool(_reproductive_evidence(signals, _context(state)))


def _status_value(value: object) -> object:
    return value.value if hasattr(value, "value") else value


def _trusted_signals(value: object) -> dict[str, object]:
    if type(value) is not dict:
        return {}
    return {
        code: normalize_signal_observation(raw)
        for code, raw in value.items()
        if type(code) is str
    }


def _is_unresolved(value: object) -> bool:
    return value is None or value == "UNKNOWN" or value == "CONFLICTED"
