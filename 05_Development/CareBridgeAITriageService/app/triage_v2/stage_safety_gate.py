"""Early stage-specific RED gate using only canonical registry rules."""

from __future__ import annotations

from typing import Mapping

from app.context import CareStage
from app.rules.condition import Tri, evaluate_condition
from app.rules.registry import Rule, RuleRegistry, get_registry, sorted_by_precedence
from app.triage_v2.state import matched_rule_trace_to_audit
from app.triage_v2.signal_normalizer import normalize_signal_observation
from app.rules.evaluator import MatchedRuleTrace

_STAGE_MAP = {
    CareStage.PRECONCEPTION.value: "PRECONCEPTION",
    CareStage.POSSIBLE_PREGNANCY.value: "POSSIBLE_PREGNANCY",
    CareStage.PREGNANCY.value: "PREGNANCY",
    CareStage.POSTPARTUM_MOTHER.value: "POSTPARTUM",
}


def stage_safety_gate(
    state: Mapping[str, object], *, registry: RuleRegistry | None = None
) -> dict[str, object]:
    if state.get("triageOutcome") == "RED":
        return {"stopConversation": True, "candidateQuestionIds": [], "plannedQuestionIds": []}
    stage_value = state.get("stage")
    raw_stage = stage_value.value if hasattr(stage_value, "value") else stage_value
    stage = _STAGE_MAP.get(raw_stage)
    if stage is None:
        return {}
    try:
        active = registry or get_registry()
    except Exception:
        return {}
    raw_signals = state.get("signals") if type(state.get("signals")) is dict else {}
    signals = {
        code: normalize_signal_observation(raw)
        for code, raw in raw_signals.items()
        if type(code) is str
    }
    context = {
        "stage": stage,
        "gestational_week": state.get("gestationalWeek"),
        "postpartum_day": state.get("postpartumDay"),
        "possible_pregnancy": state.get("possiblePregnancy"),
        **(state.get("measurements") if type(state.get("measurements")) is dict else {}),
    }
    hits = sorted_by_precedence(
        [
            rule
            for rule in active.for_stage(stage)
            if rule.rule_type == "STAGE_RED"
            and evaluate_condition(rule.condition, signals, context) is Tri.TRUE
        ]
    )
    if not hits:
        return {}
    traces = [
        MatchedRuleTrace(
            rule_id=rule.rule_id,
            outcome=rule.outcome,
            priority=rule.priority,
            rule_version=rule.rule_version,
            role="DECISIVE" if index == 0 else "CONCURRING",
        )
        for index, rule in enumerate(hits)
    ]
    return {
        "triageOutcome": "RED",
        "requiredAction": hits[0].action_code,
        "reasonCodes": list(dict.fromkeys(rule.reason_code for rule in hits)),
        "stopConversation": True,
        "decisiveRuleIds": [rule.rule_id for rule in hits],
        "allMatchedRules": [matched_rule_trace_to_audit(trace) for trace in traces],
        "rulesetVersion": active.ruleset_version,
        "rulesetHash": active.ruleset_sha256,
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "finalResponse": None,
        "readingLinks": [],
    }
