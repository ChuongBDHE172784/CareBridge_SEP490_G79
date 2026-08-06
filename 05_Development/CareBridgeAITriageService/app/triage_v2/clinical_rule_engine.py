"""V2 adapter for the canonical deterministic clinical rule evaluator."""

from __future__ import annotations

from typing import Mapping

from app.context import CareStage, IntentType
from app.rules.evaluator import CompletionReason, evaluate
from app.rules.registry import RuleRegistry, get_registry
from app.triage_v2.state import matched_rule_trace_to_audit
from app.triage_v2.signal_normalizer import normalize_signal_observation

_TRIAGE_INTENTS = {IntentType.SYMPTOM_TRIAGE.value, IntentType.FOLLOW_UP_ANSWER.value}
#: Graph stage -> registry stage. POSTPARTUM_MOTHER is renamed because the registry still stores
#: the maternal postpartum rules under the legacy name; the paediatric stages map to themselves.
#: A stage missing here evaluates no rule at all, which is how the paediatric rules stayed inert
#: after being added to the registry.
_STAGE_MAP = {
    CareStage.PRECONCEPTION.value: "PRECONCEPTION",
    CareStage.POSSIBLE_PREGNANCY.value: "POSSIBLE_PREGNANCY",
    CareStage.PREGNANCY.value: "PREGNANCY",
    CareStage.POSTPARTUM_MOTHER.value: "POSTPARTUM",
    CareStage.INFANT_0_12M.value: "INFANT_0_12M",
    CareStage.TODDLER_12_24M.value: "TODDLER_12_24M",
}


def clinical_rule_engine(
    state: Mapping[str, object], *, registry: RuleRegistry | None = None
) -> dict[str, object]:
    """Evaluate structured state with the one canonical engine, never an LLM."""

    if state.get("triageOutcome") in {"RED", "OUT_OF_SCOPE"} and state.get("stopConversation") is True:
        return {"candidateQuestionIds": [], "plannedQuestionIds": []}

    intent = _value(state.get("intent"))
    if intent not in _TRIAGE_INTENTS:
        if intent in {"", IntentType.UNKNOWN.value, IntentType.CONFLICTED.value}:
            return {
                "triageOutcome": None,
                "requiredAction": None,
                "stopConversation": False,
                "plannedQuestionIds": [],
            }
        return {
            "triageOutcome": None,
            "requiredAction": None,
            "stopConversation": False,
            "candidateQuestionIds": [],
            "plannedQuestionIds": [],
        }

    stage = _STAGE_MAP.get(_value(state.get("stage")))
    if stage is None:
        if _value(state.get("contextResolutionStatus")) != "RESOLVED":
            return {
                "triageOutcome": None,
                "requiredAction": None,
                "stopConversation": False,
                "plannedQuestionIds": [],
            }
        return _coverage_limited(state)

    try:
        active = registry or get_registry()
        result = evaluate(
            stage=stage,
            signals=_trusted_signals(state.get("signals")),
            context=_context(state, stage),
            question_round=state.get("questionRound") if type(state.get("questionRound")) is int else 0,
            minimum_dataset_complete=False,
            reproductive_relevance_hint=True,
            context_resolution_status=_value(state.get("contextResolutionStatus")),
            registry=active,
        )
    except Exception:
        return _registry_failure(state)

    outcome = result.outcome
    action = result.action_code
    stop = result.stop_conversation
    completion = result.completion_reason
    if outcome == "GREEN":
        outcome = "NEEDS_MORE_INFO"
        action = "ROUTE_TO_HEALTHCARE_WORKER"
        stop = True
        completion = CompletionReason.RULESET_COVERAGE_LIMITATION.value

    return {
        "triageOutcome": outcome,
        "requiredAction": action,
        "reasonCodes": list(result.reason_codes),
        "stopConversation": stop,
        "decisiveRuleIds": list(result.decisive_rule_ids),
        "allMatchedRules": [matched_rule_trace_to_audit(trace) for trace in result.all_matched_rules],
        "candidateQuestionIds": list(result.question_ids),
        "plannedQuestionIds": [],
        "safetyScreenStatus": result.safety_screen_status,
        "contextDatasetStatus": result.context_dataset_status,
        "greenEligibilityDatasetStatus": result.green_eligibility_dataset_status,
        "scopeStatus": result.scope_status,
        "pendingRiskStatuses": list(result.pending_risk_statuses),
        "primaryPendingRiskStatus": result.primary_pending_risk_status,
        "completionReason": completion,
        # Union, not replacement. The dataset calculator already listed what this stage needs
        # before any rule matched; overwriting it with the matched rules' own required fields
        # emptied the list whenever nothing matched yet, which left the planner with no field to
        # resolve and so nothing to ask — a baby complaint arrived and was asked nothing at all.
        "missingRequiredFields": list(dict.fromkeys([
            *(state.get("missingRequiredFields") or []),
            *result.required_fields,
        ])),
        "dataConflicts": list(dict.fromkeys([*state.get("dataConflicts", []), *result.data_conflicts])),
        "rulesetVersion": result.ruleset_version,
        "rulesetHash": result.ruleset_hash,
        "finalResponse": None,
        "readingLinks": [],
    }


def _context(state: Mapping[str, object], stage: str) -> dict[str, object]:
    measurements = state.get("measurements") if type(state.get("measurements")) is dict else {}
    return {
        # The normalizer wraps a bare reading as {"value": 38.5} so it can also carry a status.
        # Rule conditions compare against the number itself, so a wrapped reading silently failed
        # every numeric threshold — which is how the young-infant fever rule never fired.
        **{code: _measurement_value(value) for code, value in measurements.items()},
        "stage": stage,
        "gestational_week": state.get("gestationalWeek"),
        "postpartum_day": state.get("postpartumDay"),
        "baby_age_months": state.get("babyAgeMonths"),
        "possible_pregnancy": state.get("possiblePregnancy"),
    }


def _measurement_value(value: object) -> object:
    """Unwrap a normalized measurement to the number a rule threshold can compare."""

    if type(value) is dict:
        inner = value.get("value")
        return inner if type(inner) in {int, float} else value.get("status", value)
    return value


def _coverage_limited(state: Mapping[str, object]) -> dict[str, object]:
    return {
        "triageOutcome": "NEEDS_MORE_INFO",
        "requiredAction": "ROUTE_TO_HEALTHCARE_WORKER",
        "stopConversation": True,
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "completionReason": CompletionReason.RULESET_COVERAGE_LIMITATION,
        "finalResponse": None,
        "readingLinks": [],
    }


def _registry_failure(state: Mapping[str, object]) -> dict[str, object]:
    errors = list(state.get("processingErrors", []))
    errors.append(
        {"code": "RULE_ENGINE_UNAVAILABLE", "node": "clinical_rule_engine", "message": "unavailable"}
    )
    return {
        "triageOutcome": "NEEDS_MORE_INFO",
        "requiredAction": "SYSTEM_UNAVAILABLE",
        "stopConversation": True,
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "completionReason": CompletionReason.SYSTEM_FALLBACK,
        "processingErrors": errors,
        "finalResponse": None,
        "readingLinks": [],
    }


def _value(value: object) -> str:
    raw = value.value if hasattr(value, "value") else value
    return raw if type(raw) is str else ""


def _trusted_signals(value: object) -> dict[str, object]:
    if type(value) is not dict:
        return {}
    return {
        code: normalize_signal_observation(raw)
        for code, raw in value.items()
        if type(code) is str
    }
