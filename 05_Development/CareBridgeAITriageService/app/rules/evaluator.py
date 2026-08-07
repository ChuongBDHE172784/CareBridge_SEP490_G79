"""The deterministic clinical rule engine — the only component that sets an outcome.

Decision order (safety-ordered, and deliberately NOT the order of an earlier draft that
put OUT_OF_SCOPE ahead of YELLOW):

    1. Non-clinical safety policies + global RED
    2. Stage-specific RED
    3. Pending RED  — a RED rule that is TRUE on one signal and merely unanswered on
                      another. Never left hanging: it drives the next question, or
                      escalates once the round budget is spent.
    4. YELLOW
    5. NEEDS_MORE_INFO from a rule, a data conflict, or a GREEN blocker
    6. OUT_OF_SCOPE — only with no RED, no YELLOW, no reproductive signal and no
                      reproductive context
    7. GREEN — only behind the release gate, with the minimum dataset complete and no
               blocker raised
    8. Otherwise NEEDS_MORE_INFO / ROUTE_TO_HEALTHCARE_WORKER

Nothing here reads free text, prompts or model output — only structured signals and
resolved context — which is what makes prompt injection inert against the outcome.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Mapping

from app.rules.condition import (
    Presence,
    Tri,
    UNRESOLVED_PRESENCES,
    collect_leaf_states,
    evaluate_condition,
    signal_presence,
)
from app.rules.registry import (
    GreenBlocker,
    Rule,
    RuleRegistry,
    SafetyPolicy,
    get_registry,
    load_dataset_requirements,
    sorted_by_precedence,
)

ASK_CLARIFYING_QUESTIONS = "ASK_CLARIFYING_QUESTIONS"
ROUTE_TO_HEALTHCARE_WORKER = "ROUTE_TO_HEALTHCARE_WORKER"

PREDICATE_MISSING_FIELDS = "MISSING_REQUIRED_FIELDS"
PREDICATE_OUT_OF_SCOPE = "NO_REPRODUCTIVE_RELEVANCE_AND_NO_GLOBAL_RED"
PREDICATE_GREEN = "MINIMUM_DATASET_COMPLETE_AND_NO_HIGHER_RULE_MATCHED"

#: Signals that make a case reproductive regardless of any caller-supplied classification.
REPRODUCTIVE_SIGNALS = frozenset({
    "HEAVY_VAGINAL_BLEEDING", "VAGINAL_BLEEDING", "HEAVY_POSTPARTUM_BLEEDING",
    "LARGE_CLOTS", "REDUCED_FETAL_MOVEMENT", "FOUL_SMELLING_LOCHIA",
    "ABDOMINAL_PAIN", "SEVERE_ABDOMINAL_PAIN",
})
REPRODUCTIVE_CONTEXT_FIELDS = frozenset({
    "gestational_week", "postpartum_day", "possible_pregnancy", "delivery_method",
    "bleeding_amount", "last_menstrual_period_or_test",
})


class DatasetStatus(str, Enum):
    COMPLETE = "COMPLETE"
    INCOMPLETE = "INCOMPLETE"
    CONFLICTED = "CONFLICTED"
    NOT_APPLICABLE = "NOT_APPLICABLE"


class ScopeStatus(str, Enum):
    IN_SCOPE = "IN_SCOPE"
    POSSIBLY_IN_SCOPE = "POSSIBLY_IN_SCOPE"
    CONFIRMED_OUT_OF_SCOPE = "CONFIRMED_OUT_OF_SCOPE"
    CONFLICTED = "CONFLICTED"
    UNKNOWN = "UNKNOWN"


class PendingRiskStatus(str, Enum):
    UNRESOLVED_RED_CONDITION = "UNRESOLVED_RED_CONDITION"
    UNRESOLVED_SAFETY_BLOCKER = "UNRESOLVED_SAFETY_BLOCKER"
    UNRESOLVED_CONTEXT = "UNRESOLVED_CONTEXT"
    UNRESOLVED_GLOBAL_SAFETY_SCREEN = "UNRESOLVED_GLOBAL_SAFETY_SCREEN"


class CompletionReason(str, Enum):
    DATA_REQUIRED = "DATA_REQUIRED"
    MAX_QUESTION_ROUNDS_REACHED = "MAX_QUESTION_ROUNDS_REACHED"
    UNRESOLVED_HIGH_RISK_SIGNAL = "UNRESOLVED_HIGH_RISK_SIGNAL"
    UNRESOLVED_GLOBAL_SAFETY_SCREEN = "UNRESOLVED_GLOBAL_SAFETY_SCREEN"
    SCOPE_CONFLICT = "SCOPE_CONFLICT"
    RULESET_COVERAGE_LIMITATION = "RULESET_COVERAGE_LIMITATION"
    SYSTEM_FALLBACK = "SYSTEM_FALLBACK"


class SuppressionReason(str, Enum):
    DATA_AMBIGUITY = "DATA_AMBIGUITY"
    DATA_CONFLICT = "DATA_CONFLICT"
    EXPLICIT_RULE_EXCLUSION = "EXPLICIT_RULE_EXCLUSION"
    OTHER = "OTHER"


@dataclass(frozen=True)
class MatchedRuleTrace:
    """One rule's role in the decision, for the internal audit trail."""

    rule_id: str
    outcome: str
    priority: int
    rule_version: str
    role: str
    suppression_reason: str | None = None
    missing_fields: tuple[str, ...] = field(default=())


@dataclass(frozen=True)
class RuleEvaluation:
    outcome: str
    decisive_rules: tuple[Rule | SafetyPolicy, ...]
    all_matched_rules: tuple[MatchedRuleTrace, ...]
    stop_conversation: bool
    action_code: str
    reason_codes: tuple[str, ...]
    question_ids: tuple[str, ...]
    required_fields: tuple[str, ...]
    ruleset_version: str
    ruleset_hash: str
    green_blocked_by: tuple[str, ...] = field(default=())
    pending_red_rule_ids: tuple[str, ...] = field(default=())
    unresolved_signals: tuple[str, ...] = field(default=())
    data_conflicts: tuple[str, ...] = field(default=())
    # Zero-Trust Audit & Engine-Derived Fields
    safety_screen_status: str = "INCOMPLETE"
    context_dataset_status: str = "INCOMPLETE"
    green_eligibility_dataset_status: str = "INCOMPLETE"
    scope_status: str = "UNKNOWN"
    pending_risk_statuses: tuple[str, ...] = field(default=())
    primary_pending_risk_status: str | None = None
    completion_reason: str | None = None
    audit_mismatches: tuple[Mapping[str, Any], ...] = field(default=())
    triage_v2_readiness: str = "OK"

    @property
    def decisive_rule_ids(self) -> tuple[str, ...]:
        return tuple(
            getattr(rule, "rule_id", getattr(rule, "policy_id", "")) for rule in self.decisive_rules
        )

    @property
    def suppressed_rule_ids(self) -> tuple[str, ...]:
        return tuple(
            trace.rule_id for trace in self.all_matched_rules
            if trace.role.startswith("SUPPRESSED")
        )


def evaluate(
    *,
    stage: str,
    signals: Mapping[str, Any],
    context: Mapping[str, Any],
    question_round: int,
    minimum_dataset_complete: bool,
    reproductive_relevance_hint: bool = True,
    context_resolution_status: str = "RESOLVED",
    registry: RuleRegistry | None = None,
) -> RuleEvaluation:
    active = registry or get_registry()
    dataset_reqs = load_dataset_requirements()
    traces: list[MatchedRuleTrace] = []
    conflicts: list[str] = []
    audit_mismatches: list[dict[str, Any]] = []

    if context_resolution_status == "CONFLICTED":
        conflicts.append("CONTEXT_RESOLUTION_CONFLICTED")
    conflicts.extend(_signal_conflicts(signals))

    # Zero-Trust Dataset Calculations
    safety_screen_status = _calc_safety_screen_status(signals, dataset_reqs)
    context_dataset_status = _calc_context_dataset_status(stage, context, dataset_reqs)
    blockers = _active_blockers(active, stage, signals, context, conflicts)
    green_eligibility_status = _calc_green_eligibility_status(
        stage, signals, context, safety_screen_status, context_dataset_status, dataset_reqs, blockers
    )

    # Caller assertion audit checks (Never trust caller input as source of truth)
    if minimum_dataset_complete and green_eligibility_status != DatasetStatus.COMPLETE.value:
        audit_mismatches.append({
            "field": "minimumDatasetComplete",
            "callerValue": True,
            "engineValue": green_eligibility_status,
            "role": "CALLER_ASSERTION_IGNORED",
        })

    evidence = _reproductive_evidence(signals, context)
    has_reproductive_context = (
        bool(evidence) or
        context.get("gestational_week") is not None or
        context.get("postpartum_day") is not None or
        context.get("possible_pregnancy") == "YES"
    )
    if not reproductive_relevance_hint and has_reproductive_context:
        audit_mismatches.append({
            "field": "reproductiveRelevance",
            "callerValue": False,
            "engineValue": "CONFLICTED",
            "role": "CALLER_ASSERTION_IGNORED",
        })

    # --- 1. non-clinical safety policies + global RED -----------------------------
    policy_hits = [
        policy for policy in active.policies_for_stage(stage)
        if evaluate_condition(policy.condition, signals, context) is Tri.TRUE
    ]
    for policy in policy_hits:
        traces.append(MatchedRuleTrace(policy.policy_id, policy.outcome, policy.priority,
                                       policy.status, "DECISIVE"))
    
    candidates = active.for_stage(stage)
    signal_rules = [rule for rule in candidates if not rule.is_engine_rule]
    engine_rules = {rule.condition["predicate"]: rule for rule in candidates if rule.is_engine_rule}

    matched: list[Rule] = []
    pending: list[Rule] = []
    unresolved: list[str] = []

    for rule in signal_rules:
        is_ex, ex_reason, ex_missing = _exclusion_details(rule, signals, context, conflicts)
        if is_ex:
            traces.append(_trace(
                rule, "SUPPRESSED_BY_EXCLUSION",
                suppression_reason=ex_reason, missing_fields=ex_missing
            ))
            continue
        state = evaluate_condition(rule.condition, signals, context)
        if state is Tri.TRUE:
            matched.append(rule)
        elif state is Tri.UNKNOWN and rule.outcome in ("RED", "YELLOW"):
            satisfied, missing = collect_leaf_states(rule.condition, signals, context)
            if satisfied:
                pending.append(rule)
                unresolved.extend(missing)
                traces.append(_trace(rule, "PENDING_UNRESOLVED_SIGNAL"))

    red_matched = sorted_by_precedence([rule for rule in matched if rule.outcome == "RED"])
    yellow_matched = sorted_by_precedence([rule for rule in matched if rule.outcome == "YELLOW"])
    pending_red = [rule for rule in pending if rule.outcome == "RED"]

    # Scope Status Calculation
    scope_status = _calc_scope_status(
        stage=stage,
        signals=signals,
        context=context,
        safety_screen_status=safety_screen_status,
        reproductive_relevance_hint=reproductive_relevance_hint,
        has_reproductive_context=has_reproductive_context,
        policy_hits=bool(policy_hits),
        red_hits=bool(red_matched),
        pending_red_hits=bool(pending_red),
        yellow_hits=bool(yellow_matched),
        blockers=bool(blockers),
    )

    # Pending Risk Status Calculation
    pending_risks = _calc_pending_risks(
        safety_screen_status=safety_screen_status,
        context_dataset_status=context_dataset_status,
        pending_red=bool(pending_red),
        blockers=bool(blockers),
    )
    primary_pending = _determine_primary_pending_risk(pending_risks)

    if policy_hits:
        leading = min(policy_hits, key=lambda p: (p.decision_order, p.policy_id))
        return _verdict("RED", (leading,), traces, active,
                        stop=True, action=leading.action_code,
                        reasons=(leading.reason_code,), conflicts=tuple(conflicts),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        mismatches=tuple(audit_mismatches))

    if red_matched:
        for rule in red_matched:
            traces.append(_trace(rule, "DECISIVE" if rule is red_matched[0] else "CONCURRING"))
        for rule in matched:
            if rule.outcome != "RED":
                traces.append(_trace(rule, "SUPPRESSED_BY_HIGHER_SEVERITY"))
        return _verdict("RED", red_matched, traces, active, stop=True,
                        action=red_matched[0].action_code,
                        reasons=tuple(dict.fromkeys(r.reason_code for r in red_matched)),
                        questions=(), required=_union(red_matched, "required_fields"),
                        conflicts=tuple(conflicts),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        mismatches=tuple(audit_mismatches))

    # --- 3. pending RED -----------------------------------------------------------
    if pending_red:
        exhausted = question_round >= active.maximum_question_rounds
        comp_reason = (
            CompletionReason.UNRESOLVED_HIGH_RISK_SIGNAL.value if exhausted
            else CompletionReason.DATA_REQUIRED.value
        )
        readiness = "BLOCKED_CLINICAL_DISPOSITION" if exhausted else "OK"
        return _verdict(
            "NEEDS_MORE_INFO", (), traces, active,
            stop=exhausted,
            action=ROUTE_TO_HEALTHCARE_WORKER if exhausted else ASK_CLARIFYING_QUESTIONS,
            reasons=("UNRESOLVED_RED_SIGNAL",),
            questions=_union(pending_red, "question_ids"),
            required=_union(pending_red, "required_fields"),
            conflicts=tuple(conflicts),
            pending_red=tuple(rule.rule_id for rule in pending_red),
            unresolved=tuple(dict.fromkeys(unresolved)),
            safety_status=safety_screen_status, context_status=context_dataset_status,
            green_status=green_eligibility_status, scope_stat=scope_status,
            pending_risks=pending_risks, primary_pending=primary_pending,
            comp_reason=comp_reason, readiness=readiness,
            mismatches=tuple(audit_mismatches),
        )

    # --- 4. YELLOW ----------------------------------------------------------------
    if yellow_matched:
        for rule in yellow_matched:
            traces.append(_trace(rule, "DECISIVE" if rule is yellow_matched[0] else "CONCURRING"))
        return _verdict("YELLOW", yellow_matched, traces, active,
                        stop=yellow_matched[0].stop_on_match, action=yellow_matched[0].action_code,
                        reasons=tuple(dict.fromkeys(r.reason_code for r in yellow_matched)),
                        questions=_union(yellow_matched, "question_ids"),
                        required=_union(yellow_matched, "required_fields"),
                        conflicts=tuple(conflicts),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        mismatches=tuple(audit_mismatches))

    # --- 5. OUT_OF_SCOPE & SCOPE CONFLICT -----------------------------------------
    out_of_scope = engine_rules.get(PREDICATE_OUT_OF_SCOPE)
    if scope_status == ScopeStatus.CONFIRMED_OUT_OF_SCOPE.value and out_of_scope is not None:
        traces.append(_trace(out_of_scope, "DECISIVE"))
        return _verdict("OUT_OF_SCOPE", (out_of_scope,), traces, active,
                        stop=out_of_scope.stop_on_match, action=out_of_scope.action_code,
                        reasons=(out_of_scope.reason_code,), conflicts=tuple(conflicts),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        mismatches=tuple(audit_mismatches))
    elif scope_status == ScopeStatus.CONFLICTED.value:
        reasons_list = ["SCOPE_CLASSIFICATION_CONFLICT"] + list(conflicts)
        return _verdict("NEEDS_MORE_INFO", (), traces, active,
                        stop=question_round >= active.maximum_question_rounds,
                        action=ASK_CLARIFYING_QUESTIONS,
                        reasons=tuple(dict.fromkeys(reasons_list)),
                        conflicts=tuple(conflicts) + ("SCOPE_CLASSIFICATION_CONFLICT",),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        comp_reason=CompletionReason.SCOPE_CONFLICT.value,
                        mismatches=tuple(audit_mismatches))

    # --- 6. NEEDS_MORE_INFO -------------------------------------------------------
    needs_info = sorted_by_precedence(
        [rule for rule in matched if rule.outcome == "NEEDS_MORE_INFO"]
    )
    missing_fields_rule = engine_rules.get(PREDICATE_MISSING_FIELDS)
    if missing_fields_rule is not None and green_eligibility_status != DatasetStatus.COMPLETE.value:
        needs_info = sorted_by_precedence(list(needs_info) + [missing_fields_rule])

    if needs_info or blockers or conflicts or safety_screen_status != DatasetStatus.COMPLETE.value:
        exhausted = question_round >= active.maximum_question_rounds
        leading = needs_info[0] if needs_info else None
        for rule in needs_info:
            traces.append(_trace(rule, "DECISIVE" if rule is leading else "CONCURRING"))
        reasons = list(dict.fromkeys(rule.reason_code for rule in needs_info))
        reasons.extend(blocker.reason_code for blocker in blockers)
        reasons.extend(conflicts)
        if safety_screen_status == DatasetStatus.INCOMPLETE.value:
            reasons.append("SAFETY_SCREEN_INCOMPLETE")
        action = ASK_CLARIFYING_QUESTIONS
        comp_reason = CompletionReason.DATA_REQUIRED.value
        if exhausted:
            action = ROUTE_TO_HEALTHCARE_WORKER
            reasons.append("MAX_QUESTION_ROUNDS_EXHAUSTED")
            comp_reason = (
                CompletionReason.UNRESOLVED_GLOBAL_SAFETY_SCREEN.value
                if safety_screen_status == DatasetStatus.INCOMPLETE.value
                else CompletionReason.MAX_QUESTION_ROUNDS_REACHED.value
            )
        questions = list(_union(needs_info, "question_ids"))
        for blocker in blockers:
            questions.extend(blocker.question_ids)
        return _verdict("NEEDS_MORE_INFO", tuple(needs_info), traces, active,
                        stop=exhausted, action=action,
                        reasons=tuple(dict.fromkeys(reasons)),
                        questions=tuple(dict.fromkeys(questions)),
                        required=_union(needs_info, "required_fields"),
                        conflicts=tuple(conflicts),
                        green_blocked=tuple(b.blocker_id for b in blockers),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        comp_reason=comp_reason,
                        mismatches=tuple(audit_mismatches))

    # --- 7. GREEN, behind release gate --------------------------------------------
    green = engine_rules.get(PREDICATE_GREEN)
    if green is not None and green_eligibility_status == DatasetStatus.COMPLETE.value:
        if not active.green_enabled:
            traces.append(_trace(green, "SUPPRESSED_BY_RELEASE_GATE"))
            return _verdict("NEEDS_MORE_INFO", (), traces, active, stop=True,
                            action=ROUTE_TO_HEALTHCARE_WORKER,
                            reasons=("GREEN_RELEASE_GATE_DISABLED",),
                            conflicts=tuple(conflicts),
                            safety_status=safety_screen_status, context_status=context_dataset_status,
                            green_status=green_eligibility_status, scope_stat=scope_status,
                            pending_risks=pending_risks, primary_pending=primary_pending,
                            comp_reason=CompletionReason.RULESET_COVERAGE_LIMITATION.value,
                            mismatches=tuple(audit_mismatches))
        traces.append(_trace(green, "DECISIVE"))
        return _verdict("GREEN", (green,), traces, active, stop=green.stop_on_match,
                        action=green.action_code, reasons=(green.reason_code,),
                        conflicts=tuple(conflicts),
                        safety_status=safety_screen_status, context_status=context_dataset_status,
                        green_status=green_eligibility_status, scope_stat=scope_status,
                        pending_risks=pending_risks, primary_pending=primary_pending,
                        mismatches=tuple(audit_mismatches))

    # --- 8. Default Fallback -------------------------------------------------------
    return _verdict("NEEDS_MORE_INFO", (), traces, active, stop=True,
                    action=ROUTE_TO_HEALTHCARE_WORKER,
                    reasons=("RULESET_CANNOT_STRATIFY",), conflicts=tuple(conflicts),
                    safety_status=safety_screen_status, context_status=context_dataset_status,
                    green_status=green_eligibility_status, scope_stat=scope_status,
                    pending_risks=pending_risks, primary_pending=primary_pending,
                    comp_reason=CompletionReason.RULESET_COVERAGE_LIMITATION.value,
                    mismatches=tuple(audit_mismatches))


def _calc_safety_screen_status(signals: Mapping[str, Any], dataset_reqs: Mapping[str, Any]) -> str:
    req_codes = dataset_reqs.get("globalSafety", {}).get("requiredSignalCodes", [])
    has_conflicted = False
    all_resolved = True

    for code in req_codes:
        pres = signal_presence(signals, code)
        if pres is Presence.CONFLICTED:
            has_conflicted = True
        if pres in (Presence.UNKNOWN, Presence.CONFLICTED, Presence.UNAWARE_OR_UNMEASURABLE):
            all_resolved = False

    if has_conflicted:
        return DatasetStatus.CONFLICTED.value
    if all_resolved:
        return DatasetStatus.COMPLETE.value
    return DatasetStatus.INCOMPLETE.value


def _calc_context_dataset_status(stage: str, context: Mapping[str, Any], dataset_reqs: Mapping[str, Any]) -> str:
    if stage not in ("PRECONCEPTION", "POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM"):
        return DatasetStatus.INCOMPLETE.value

    # Contradiction check
    if stage == "PREGNANCY" and context.get("postpartum_day") not in (None, "UNKNOWN"):
        return DatasetStatus.CONFLICTED.value
    if stage == "POSTPARTUM" and context.get("gestational_week") not in (None, "UNKNOWN"):
        return DatasetStatus.CONFLICTED.value
    if stage == "POSTPARTUM" and context.get("possible_pregnancy") == "YES":
        return DatasetStatus.CONFLICTED.value

    req_fields = dataset_reqs.get("byStage", {}).get(stage, {}).get("contextFields", [])
    for f_name in req_fields:
        if f_name == "stage":
            continue
        val = context.get(f_name)
        if val in (None, "UNKNOWN"):
            return DatasetStatus.INCOMPLETE.value

    return DatasetStatus.COMPLETE.value


def _calc_green_eligibility_status(
    stage: str,
    signals: Mapping[str, Any],
    context: Mapping[str, Any],
    safety_screen_status: str,
    context_dataset_status: str,
    dataset_reqs: Mapping[str, Any],
    blockers: Sequence[GreenBlocker],
) -> str:
    if safety_screen_status != DatasetStatus.COMPLETE.value or context_dataset_status != DatasetStatus.COMPLETE.value:
        return DatasetStatus.CONFLICTED.value if (
            safety_screen_status == DatasetStatus.CONFLICTED.value or context_dataset_status == DatasetStatus.CONFLICTED.value
        ) else DatasetStatus.INCOMPLETE.value

    req_green = dataset_reqs.get("byStage", {}).get(stage, {}).get("greenEligibilityFields", [])
    for f_name in req_green:
        if f_name == "stage":
            continue
        val = context.get(f_name)
        if val in (None, "UNKNOWN"):
            return DatasetStatus.INCOMPLETE.value

    if blockers:
        return DatasetStatus.INCOMPLETE.value

    return DatasetStatus.COMPLETE.value


def _calc_scope_status(
    *,
    stage: str,
    signals: Mapping[str, Any],
    context: Mapping[str, Any],
    safety_screen_status: str,
    reproductive_relevance_hint: bool,
    has_reproductive_context: bool,
    policy_hits: bool,
    red_hits: bool,
    pending_red_hits: bool,
    yellow_hits: bool,
    blockers: bool,
) -> str:
    if not reproductive_relevance_hint and has_reproductive_context:
        return ScopeStatus.CONFLICTED.value

    can_be_oos = (
        safety_screen_status == DatasetStatus.COMPLETE.value and
        not policy_hits and
        not red_hits and
        not pending_red_hits and
        not yellow_hits and
        not has_reproductive_context and
        not blockers and
        context.get("possible_pregnancy") != "UNKNOWN" and
        not reproductive_relevance_hint
    )
    if can_be_oos:
        return ScopeStatus.CONFIRMED_OUT_OF_SCOPE.value

    if stage in ("POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM") or has_reproductive_context:
        return ScopeStatus.POSSIBLY_IN_SCOPE.value

    return ScopeStatus.IN_SCOPE.value


def _calc_pending_risks(
    *,
    safety_screen_status: str,
    context_dataset_status: str,
    pending_red: bool,
    blockers: bool,
) -> tuple[str, ...]:
    risks: list[str] = []
    if pending_red:
        risks.append(PendingRiskStatus.UNRESOLVED_RED_CONDITION.value)
    if safety_screen_status == DatasetStatus.INCOMPLETE.value:
        risks.append(PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN.value)
    if blockers:
        risks.append(PendingRiskStatus.UNRESOLVED_SAFETY_BLOCKER.value)
    if context_dataset_status == DatasetStatus.INCOMPLETE.value:
        risks.append(PendingRiskStatus.UNRESOLVED_CONTEXT.value)
    return tuple(risks)


def _determine_primary_pending_risk(pending_risks: tuple[str, ...]) -> str | None:
    order = (
        PendingRiskStatus.UNRESOLVED_RED_CONDITION.value,
        PendingRiskStatus.UNRESOLVED_GLOBAL_SAFETY_SCREEN.value,
        PendingRiskStatus.UNRESOLVED_SAFETY_BLOCKER.value,
        PendingRiskStatus.UNRESOLVED_CONTEXT.value,
    )
    for target in order:
        if target in pending_risks:
            return target
    return None


def _trace(
    rule: Rule,
    role: str,
    suppression_reason: str | None = None,
    missing_fields: tuple[str, ...] = (),
) -> MatchedRuleTrace:
    return MatchedRuleTrace(
        rule.rule_id, rule.outcome, rule.priority, rule.rule_version, role,
        suppression_reason=suppression_reason, missing_fields=missing_fields
    )


def _union(rules, attribute: str) -> tuple[str, ...]:
    return tuple(dict.fromkeys(value for rule in rules for value in getattr(rule, attribute)))


def _exclusion_details(
    rule: Rule, signals: Mapping[str, Any], context: Mapping[str, Any], conflicts: list[str]
) -> tuple[bool, str | None, tuple[str, ...]]:
    for predicate in rule.exclusion_predicates:
        if predicate == "BLEEDING_AMOUNT_UNKNOWN_OR_CONFLICTED":
            amount = context.get("bleeding_amount")
            if amount in (None, "UNKNOWN"):
                return True, SuppressionReason.DATA_AMBIGUITY.value, ("bleeding_amount",)
            elif amount == "CONFLICTED":
                return True, SuppressionReason.DATA_CONFLICT.value, ("bleeding_amount",)
        elif predicate == "CONTEXT_RESOLUTION_CONFLICTED":
            if "CONTEXT_RESOLUTION_CONFLICTED" in conflicts:
                return True, SuppressionReason.DATA_CONFLICT.value, ()
    return False, None, ()


def _signal_conflicts(signals: Mapping[str, Any]) -> list[str]:
    return [
        f"SIGNAL_CONFLICTED:{code}"
        for code in signals
        if signal_presence(signals, code) is Presence.CONFLICTED
    ]


def _active_blockers(
    registry: RuleRegistry, stage: str, signals: Mapping[str, Any], context: Mapping[str, Any], conflicts: list[str]
) -> tuple[GreenBlocker, ...]:
    hits: list[GreenBlocker] = []
    for blocker in registry.blockers_for_stage(stage):
        if blocker.condition is not None:
            if evaluate_condition(blocker.condition, signals, context) is Tri.TRUE:
                hits.append(blocker)
        elif blocker.predicate == "BLEEDING_AMOUNT_UNKNOWN_OR_CONFLICTED":
            if context.get("bleeding_amount") in ("UNKNOWN", "CONFLICTED"):
                hits.append(blocker)
        elif blocker.predicate == "CONTEXT_RESOLUTION_CONFLICTED":
            if "CONTEXT_RESOLUTION_CONFLICTED" in conflicts:
                hits.append(blocker)
    return tuple(hits)


def _reproductive_evidence(signals: Mapping[str, Any], context: Mapping[str, Any]) -> tuple[str, ...]:
    evidence = [
        code for code in signals
        if code in REPRODUCTIVE_SIGNALS
        and signal_presence(signals, code) is Presence.PRESENT
    ]
    for field_name, val in context.items():
        if field_name not in REPRODUCTIVE_CONTEXT_FIELDS or val in (None, "UNKNOWN", "NONE", "NO"):
            continue
        evidence.append(field_name)
    return tuple(dict.fromkeys(evidence))


def _verdict(
    outcome: str,
    decisive,
    traces: list[MatchedRuleTrace],
    registry: RuleRegistry,
    *,
    stop: bool,
    action: str,
    reasons: tuple[str, ...] = (),
    questions: tuple[str, ...] = (),
    required: tuple[str, ...] = (),
    conflicts: tuple[str, ...] = (),
    green_blocked: tuple[str, ...] = (),
    pending_red: tuple[str, ...] = (),
    unresolved: tuple[str, ...] = (),
    safety_status: str = "INCOMPLETE",
    context_status: str = "INCOMPLETE",
    green_status: str = "INCOMPLETE",
    scope_stat: str = "UNKNOWN",
    pending_risks: tuple[str, ...] = (),
    primary_pending: str | None = None,
    comp_reason: str | None = None,
    readiness: str = "OK",
    mismatches: tuple[Mapping[str, Any], ...] = (),
) -> RuleEvaluation:
    return RuleEvaluation(
        outcome=outcome,
        decisive_rules=tuple(decisive),
        all_matched_rules=tuple(traces),
        stop_conversation=True if outcome == "RED" else stop,
        action_code=action,
        reason_codes=reasons,
        question_ids=questions,
        required_fields=required,
        ruleset_version=registry.ruleset_version,
        ruleset_hash=registry.ruleset_sha256,
        green_blocked_by=green_blocked,
        pending_red_rule_ids=pending_red,
        unresolved_signals=unresolved,
        data_conflicts=conflicts,
        safety_screen_status=safety_status,
        context_dataset_status=context_status,
        green_eligibility_dataset_status=green_status,
        scope_status=scope_stat,
        pending_risk_statuses=pending_risks,
        primary_pending_risk_status=primary_pending,
        completion_reason=comp_reason,
        audit_mismatches=mismatches,
        triage_v2_readiness=readiness,
    )
