"""Stage- and entity-independent deterministic safety gate for Triage V2."""

from __future__ import annotations

from typing import Any, Mapping

from app.rules.condition import Presence, Tri, evaluate_condition, signal_presence
from app.rules.evaluator import CompletionReason, DatasetStatus, PendingRiskStatus
from app.rules.registry import Rule, RuleRegistry, SafetyPolicy, get_registry
from app.triage_v2.signal_normalizer import normalize_signal_observation


def global_safety_gate(
    state: Mapping[str, object], *, registry: RuleRegistry | None = None
) -> dict[str, object]:
    """Return a partial update from canonical global safety semantics only.

    The node intentionally consumes structured signals rather than free text. Boolean
    shorthand is excluded at this trust boundary; P2-T7 owns signal normalization.
    """

    if state.get("triageOutcome") == "RED":
        return {"stopConversation": True}

    try:
        active = registry or get_registry()
    except Exception:
        return _registry_failure(state)

    signals = _trusted_presence_signals(state.get("signals"))
    safety_status = _safety_status(signals, active)
    pending = _pending_statuses(state, safety_status)

    policies = [
        policy
        for policy in active.safety_policies
        if policy.enabled and evaluate_condition(policy.condition, signals, {}) is Tri.TRUE
    ]
    rules = [
        rule
        for rule in active.rules
        if rule.rule_type == "GLOBAL_RED"
        and not rule.is_engine_rule
        and evaluate_condition(rule.condition, signals, {}) is Tri.TRUE
    ]
    hits: list[Rule | SafetyPolicy] = sorted(
        [*policies, *rules], key=lambda hit: (hit.decision_order, -hit.priority, _hit_id(hit))
    )

    updates: dict[str, object] = {
        "safetyScreenStatus": DatasetStatus(safety_status),
        "pendingRiskStatuses": pending,
        "primaryPendingRiskStatus": _primary_pending(pending),
        "rulesetVersion": active.ruleset_version,
        "rulesetHash": active.ruleset_sha256,
        # A resumed turn must never inherit a previous reassuring disposition before
        # the safety gate and downstream nodes have evaluated the new evidence.
        "triageOutcome": None,
        "requiredAction": None,
        "stopConversation": False,
        "decisiveRuleIds": [],
        "reasonCodes": [],
        "allMatchedRules": [],
        "completionReason": None,
        "missingRequiredFields": [],
        "candidateQuestionIds": [],
        "plannedQuestionIds": [],
        "finalResponse": None,
        "citations": [],
        "readingLinks": [],
    }
    if not hits:
        return updates

    leading = hits[0]
    updates.update(
        triageOutcome="RED",
        requiredAction=leading.action_code,
        stopConversation=True,
        decisiveRuleIds=[_hit_id(hit) for hit in hits],
        reasonCodes=list(dict.fromkeys(hit.reason_code for hit in hits)),
        allMatchedRules=[_audit(hit, index == 0) for index, hit in enumerate(hits)],
        completionReason=None,
        missingRequiredFields=[],
        candidateQuestionIds=[],
        plannedQuestionIds=[],
        finalResponse=None,
        citations=[],
        readingLinks=[],
    )
    return updates


def _trusted_presence_signals(value: object) -> dict[str, object]:
    if type(value) is not dict:
        return {}
    trusted: dict[str, object] = {}
    for code, raw in value.items():
        if type(code) is not str:
            continue
        trusted[code] = normalize_signal_observation(raw)
    return trusted


def _safety_status(signals: Mapping[str, Any], registry: RuleRegistry) -> str:
    required = _global_signal_codes(registry)
    presences = [signal_presence(signals, code) for code in required]
    if any(presence is Presence.CONFLICTED for presence in presences):
        return DatasetStatus.CONFLICTED.value
    if required and all(
        presence not in {
            Presence.UNKNOWN,
            Presence.CONFLICTED,
            Presence.UNAWARE_OR_UNMEASURABLE,
        }
        for presence in presences
    ):
        return DatasetStatus.COMPLETE.value
    return DatasetStatus.INCOMPLETE.value


def _global_signal_codes(registry: RuleRegistry) -> tuple[str, ...]:
    codes: list[str] = []

    def visit(node: Mapping[str, Any]) -> None:
        for key in ("all", "any"):
            if key in node:
                for child in node[key]:
                    visit(child)
                return
        if "not" in node:
            visit(node["not"])
            return
        code = node.get("signal")
        if type(code) is str:
            codes.append(code)

    for rule in registry.rules:
        if rule.rule_type == "GLOBAL_RED" and not rule.is_engine_rule:
            visit(rule.condition)
    for policy in registry.safety_policies:
        if policy.enabled:
            visit(policy.condition)
    return tuple(dict.fromkeys(codes))


def _pending_statuses(
    state: Mapping[str, object], safety_status: str
) -> list[PendingRiskStatus]:
    current = [
        item
        for item in state.get("pendingRiskStatuses", [])
        if item != PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN
        and item != PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN.value
    ]
    if safety_status != DatasetStatus.COMPLETE.value:
        current.append(PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN)
    return current


def _primary_pending(items: list[object]) -> PendingRiskStatus | None:
    order = (
        PendingRiskStatus.UNRESOLVED_RED_CONDITION,
        PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN,
        PendingRiskStatus.UNRESOLVED_SAFETY_BLOCKER,
        PendingRiskStatus.UNRESOLVED_CONTEXT,
    )
    values = {item.value if isinstance(item, PendingRiskStatus) else item for item in items}
    return next((item for item in order if item.value in values), None)


def _hit_id(hit: Rule | SafetyPolicy) -> str:
    return hit.rule_id if isinstance(hit, Rule) else hit.policy_id


def _audit(hit: Rule | SafetyPolicy, decisive: bool) -> dict[str, object]:
    return {
        "ruleId": _hit_id(hit),
        "outcome": hit.outcome,
        "priority": hit.priority,
        "ruleVersion": hit.rule_version if isinstance(hit, Rule) else hit.status,
        "role": "DECISIVE" if decisive else "CONCURRING",
        "suppressionReason": None,
        "missingFields": [],
    }


def _registry_failure(state: Mapping[str, object]) -> dict[str, object]:
    errors = list(state.get("processingErrors", []))
    errors.append(
        {"code": "RULE_REGISTRY_UNAVAILABLE", "node": "global_safety_gate", "message": "unavailable"}
    )
    return {
        "triageOutcome": "NEEDS_MORE_INFO",
        "requiredAction": "SYSTEM_UNAVAILABLE",
        "stopConversation": True,
        "completionReason": CompletionReason.SYSTEM_FALLBACK,
        "processingErrors": errors,
        "finalResponse": None,
        "readingLinks": [],
    }
