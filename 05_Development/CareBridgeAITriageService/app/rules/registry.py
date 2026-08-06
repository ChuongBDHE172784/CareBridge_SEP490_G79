"""Loader for the canonical triage rule registry — fail-closed.

The registry ships as a generated copy of
``05_Development/Contracts/triage/triage_rules_v2.json`` with a ``.sha256`` sidecar
written by ``DevTools/sync_triage_rule_registry.py``.

Fail-closed is the whole point of this module. An earlier draft dropped an invalid rule
with a warning and carried on; that is unsafe, because a missing emergency rule silently
turns into "no rule matched", which is one step away from GREEN. Now: any APPROVED rule
that fails validation, any missing critical rule, any duplicate id and any digest
mismatch makes V2 UNAVAILABLE. Only a DRAFT/RETIRED rule may be skipped quietly.
"""

from __future__ import annotations

import hashlib
import json
import logging
from dataclasses import dataclass, field
from datetime import date
from pathlib import Path
from typing import Any, Mapping, Sequence

from app.rules.condition import (
    ConditionSchemaError,
    validate_condition,
    validate_engine_predicate,
)

logger = logging.getLogger(__name__)

DATA_DIR = Path(__file__).resolve().parents[2] / "data"
REGISTRY_PATH = DATA_DIR / "triage_rules_v2.json"
REQUIRED_RULE_MANIFEST_PATH = DATA_DIR / "required_rule_manifest.json"

V2_STAGES = ("PRECONCEPTION", "POSSIBLE_PREGNANCY", "PREGNANCY", "POSTPARTUM")
V2_OUTCOMES = ("RED", "YELLOW", "GREEN", "NEEDS_MORE_INFO", "OUT_OF_SCOPE")
RELEASABLE_STATUSES = ("ACTIVE",)
SKIPPABLE_STATUSES = ("DRAFT", "RETIRED", "DISABLED")

#: Legacy artifacts (ruleset <= 2.1.0) carried `status: "APPROVED"`. That name reads as
#: clinical approval, which never happened — see AI_TRIAGE_V2_DECISIONS.md D-011. The value is
#: still accepted so older artifacts load, but it is mapped to a RELEASE flag only and never
#: to any validation claim.
_LEGACY_STATUS_TO_RELEASE_STATUS = {
    "APPROVED": "ACTIVE",
    "DRAFT": "DRAFT",
    "RETIRED": "RETIRED",
}

_REQUIRED_RULE_KEYS = (
    "ruleId", "ruleVersion", "ruleType", "stages", "outcome", "priority",
    "decisionOrder", "stopOnMatch", "conditionType", "condition", "reasonCode",
    "actionCode",
)


def _release_status(payload: Mapping[str, Any], rule_id: Any) -> str | None:
    """Read `releaseStatus`, falling back to the deprecated `status` field.

    Returns ``None`` when neither field is present, which the caller treats as an integrity
    failure rather than a default.
    """

    if "releaseStatus" in payload:
        return payload["releaseStatus"]
    legacy = payload.get("status")
    if legacy is None:
        return None
    mapped = _LEGACY_STATUS_TO_RELEASE_STATUS.get(legacy)
    if mapped is None:
        return legacy  # unrecognised: let the caller reject it
    logger.warning(
        "DEPRECATED: rule %s uses legacy status=%r; mapped to releaseStatus=%r. "
        "Legacy APPROVED means RELEASE-ENABLED only and never clinical validation "
        "(clinicalValidationStatus stays NOT_CLINICALLY_VALIDATED). Re-generate the artifact.",
        rule_id, legacy, mapped,
    )
    return mapped

_OUTCOME_SEVERITY = {
    "RED": 0,
    "YELLOW": 1,
    "NEEDS_MORE_INFO": 2,
    "OUT_OF_SCOPE": 3,
    "GREEN": 4,
}


class RegistryIntegrityError(RuntimeError):
    """Raised whenever the registry cannot be trusted. V2 must be reported UNAVAILABLE."""


@dataclass(frozen=True)
class Rule:
    rule_id: str
    rule_version: str
    rule_type: str
    title: str
    stages: tuple[str, ...]
    outcome: str
    priority: int
    decision_order: int
    stop_on_match: bool
    condition_type: str
    condition: Mapping[str, Any]
    required_fields: tuple[str, ...]
    question_ids: tuple[str, ...]
    exclusion_predicates: tuple[str, ...]
    reason_code: str
    action_code: str
    source_ids: tuple[str, ...]
    status: str
    requires_release_gate: str | None = None

    @property
    def is_engine_rule(self) -> bool:
        return self.condition_type == "ENGINE"

    def applies_to_stage(self, stage: str) -> bool:
        return stage in self.stages


@dataclass(frozen=True)
class SafetyPolicy:
    """A safety behaviour that is NOT part of the clinical Rule Matrix."""

    policy_id: str
    policy_type: str
    title: str
    stages: tuple[str, ...]
    outcome: str
    priority: int
    decision_order: int
    stop_conversation: bool
    condition: Mapping[str, Any]
    reason_code: str
    action_code: str
    status: str
    enabled: bool
    review_due_at: str | None
    expires_at: str | None


@dataclass(frozen=True)
class GreenBlocker:
    """A signal the approved rule set does not stratify. Blocks GREEN, asserts nothing."""

    blocker_id: str
    title: str
    stages: tuple[str, ...]
    condition: Mapping[str, Any] | None
    predicate: str | None
    reason_code: str
    question_ids: tuple[str, ...]


@dataclass(frozen=True)
class RuleRegistry:
    ruleset_version: str
    rule_matrix_version: str
    ruleset_sha256: str
    maximum_question_rounds: int
    green_enabled: bool
    rules: tuple[Rule, ...]
    safety_policies: tuple[SafetyPolicy, ...]
    green_blockers: tuple[GreenBlocker, ...]
    runtime_policies: Mapping[str, Mapping[str, Any]]
    signal_display_text: Mapping[str, str]
    skipped_rule_ids: tuple[str, ...] = field(default=())

    def for_stage(self, stage: str) -> tuple[Rule, ...]:
        return tuple(rule for rule in self.rules if rule.applies_to_stage(stage))

    def policies_for_stage(self, stage: str) -> tuple[SafetyPolicy, ...]:
        return tuple(
            policy for policy in self.safety_policies
            if policy.enabled and stage in policy.stages
        )

    def blockers_for_stage(self, stage: str) -> tuple[GreenBlocker, ...]:
        return tuple(b for b in self.green_blockers if stage in b.stages)

    def by_id(self, rule_id: str) -> Rule | None:
        return next((rule for rule in self.rules if rule.rule_id == rule_id), None)


def _verify_integrity(path: Path) -> str:
    payload = path.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    sidecar = path.with_suffix(path.suffix + ".sha256")
    if not sidecar.is_file():
        raise RegistryIntegrityError(
            f"digest sidecar missing for {path.name}; run DevTools/sync_triage_rule_registry.py"
        )
    if sidecar.read_text(encoding="utf-8").strip() != digest:
        raise RegistryIntegrityError(
            f"{path.name} does not match its canonical digest; the runtime copy is generated — "
            "re-run DevTools/sync_triage_rule_registry.py instead of editing it"
        )
    return digest


def _as_date(value: Any) -> date | None:
    if not isinstance(value, str) or not value:
        return None
    try:
        return date.fromisoformat(value[:10])
    except ValueError:
        return None


def _is_effective(payload: Mapping[str, Any], today: date) -> bool:
    start = _as_date(payload.get("effectiveFrom"))
    if start is None or start > today:
        return False
    end = _as_date(payload.get("effectiveTo"))
    return end is None or end >= today


def _validate_rule(payload: Mapping[str, Any], today: date) -> str | None:
    for key in _REQUIRED_RULE_KEYS:
        if key not in payload:
            return f"missing required key {key!r}"
    if not payload.get("approvedAt"):
        # Provenance timestamp for the internal review, not a clinical sign-off date.
        return "releasable rule has no approvedAt timestamp"
    if not isinstance(payload["ruleVersion"], str) or payload["ruleVersion"].count(".") != 2:
        return f"malformed ruleVersion {payload.get('ruleVersion')!r}"
    if payload["outcome"] not in V2_OUTCOMES:
        return f"outcome {payload['outcome']!r} is outside the V2 contract"
    stages = payload["stages"]
    if not isinstance(stages, list) or not stages:
        return "stages must be a non-empty array"
    unsupported = [stage for stage in stages if stage not in V2_STAGES]
    if unsupported:
        return f"stages outside V2 scope: {unsupported}"
    if not isinstance(payload["priority"], int) or not 0 <= payload["priority"] <= 100:
        return f"priority {payload.get('priority')!r} out of range"
    if not isinstance(payload["decisionOrder"], int):
        return "decisionOrder must be an integer"
    if not _is_effective(payload, today):
        return "outside its effective window"
    try:
        if payload["conditionType"] == "ENGINE":
            validate_engine_predicate(payload["condition"])
        elif payload["conditionType"] == "SIGNAL":
            validate_condition(payload["condition"])
        else:
            return f"unknown conditionType {payload['conditionType']!r}"
    except ConditionSchemaError as error:
        return f"invalid condition: {error}"
    return None


def _to_rule(payload: Mapping[str, Any]) -> Rule:
    return Rule(
        rule_id=payload["ruleId"],
        rule_version=payload["ruleVersion"],
        rule_type=payload["ruleType"],
        title=payload.get("title", payload["ruleId"]),
        stages=tuple(payload["stages"]),
        outcome=payload["outcome"],
        priority=payload["priority"],
        decision_order=payload["decisionOrder"],
        stop_on_match=bool(payload["stopOnMatch"]),
        condition_type=payload["conditionType"],
        condition=payload["condition"],
        required_fields=tuple(payload.get("requiredFields", ())),
        question_ids=tuple(payload.get("questionIds", ())),
        exclusion_predicates=tuple(payload.get("exclusionPredicates", ())),
        reason_code=payload["reasonCode"],
        action_code=payload["actionCode"],
        source_ids=tuple(payload.get("sourceIds", ())),
        status=_release_status(payload, payload.get("ruleId")) or "ACTIVE",
        requires_release_gate=payload.get("requiresReleaseGate"),
    )


def load_registry(
    path: Path = REGISTRY_PATH,
    *,
    manifest_path: Path | None = None,
    today: date | None = None,
) -> RuleRegistry:
    digest = _verify_integrity(path)
    document = json.loads(path.read_text(encoding="utf-8"))
    effective_today = today or date.today()

    rules: list[Rule] = []
    skipped: list[str] = []
    seen: set[str] = set()

    for payload in document.get("rules", []):
        rule_id = payload.get("ruleId")
        status = _release_status(payload, rule_id)
        if status in SKIPPABLE_STATUSES:
            skipped.append(rule_id)
            logger.info("triage rule skipped rule_id=%s releaseStatus=%s", rule_id, status)
            continue
        if status not in RELEASABLE_STATUSES:
            raise RegistryIntegrityError(
                f"rule {rule_id} has unrecognised releaseStatus {status!r}; "
                "refusing to load the registry"
            )
        if rule_id in seen:
            raise RegistryIntegrityError(f"duplicate ruleId {rule_id!r}")
        seen.add(rule_id)
        reason = _validate_rule(payload, effective_today)
        if reason is not None:
            # An APPROVED rule that will not parse is a safety failure, not a warning.
            raise RegistryIntegrityError(f"APPROVED rule {rule_id} is invalid: {reason}")
        rules.append(_to_rule(payload))

    if not rules:
        raise RegistryIntegrityError("no releasable triage rules were loaded")

    policies = tuple(
        SafetyPolicy(
            policy_id=item["policyId"],
            policy_type=item["policyType"],
            title=item.get("title", item["policyId"]),
            stages=tuple(item["stages"]),
            outcome=item["outcome"],
            priority=item["priority"],
            decision_order=item["decisionOrder"],
            stop_conversation=bool(item.get("stopConversation", True)),
            condition=item["condition"],
            reason_code=item["reasonCode"],
            action_code=item["actionCode"],
            status=item["status"],
            enabled=bool(item.get("enabled", False)),
            review_due_at=item.get("reviewDueAt"),
            expires_at=item.get("expiresAt"),
        )
        for item in document.get("safetyPolicies", [])
    )
    for policy in policies:
        expiry = _as_date(policy.expires_at)
        if policy.enabled and expiry is not None and expiry < effective_today:
            logger.warning(
                "safety policy past its expiry policy_id=%s expired_at=%s — clinical review overdue",
                policy.policy_id, policy.expires_at,
            )

    blockers = tuple(
        GreenBlocker(
            blocker_id=item["blockerId"],
            title=item.get("title", item["blockerId"]),
            stages=tuple(item["stages"]),
            condition=item.get("condition"),
            predicate=item.get("predicate"),
            reason_code=item["reasonCode"],
            question_ids=tuple(item.get("questionIds", ())),
        )
        for item in document.get("greenSafetyBlockers", [])
    )

    manifest_file = manifest_path or REQUIRED_RULE_MANIFEST_PATH
    if manifest_file.is_file():
        manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
        missing = [rid for rid in manifest.get("criticalRuleIds", []) if rid not in seen]
        if missing:
            raise RegistryIntegrityError(f"critical rules missing from registry: {missing}")
        enabled_policies = {p.policy_id for p in policies if p.enabled}
        missing_policies = [
            pid for pid in manifest.get("criticalPolicyIds", []) if pid not in enabled_policies
        ]
        if missing_policies:
            raise RegistryIntegrityError(
                f"critical safety policies missing or disabled: {missing_policies}"
            )

    scope = document.get("scope", {})
    return RuleRegistry(
        ruleset_version=document["rulesetVersion"],
        rule_matrix_version=document["ruleMatrixVersion"],
        ruleset_sha256=digest,
        maximum_question_rounds=int(scope.get("maximumQuestionRounds", 3)),
        green_enabled=bool(document.get("releaseGates", {}).get("greenEnabled", False)),
        rules=tuple(rules),
        safety_policies=policies,
        green_blockers=blockers,
        runtime_policies=document.get("runtimePolicies", {}),
        signal_display_text={
            entry["code"]: entry["displayText"]
            for entry in document.get("signalCatalog", [])
            if isinstance(entry, Mapping) and "code" in entry and "displayText" in entry
        },
        skipped_rule_ids=tuple(skipped),
    )


_CACHED: RuleRegistry | None = None


def get_registry() -> RuleRegistry:
    global _CACHED
    if _CACHED is None:
        _CACHED = load_registry()
    return _CACHED


def reset_registry_cache() -> None:
    global _CACHED
    _CACHED = None


def load_dataset_requirements(path: Path | None = None) -> dict[str, Any]:
    target_path = path or (DATA_DIR / "dataset_requirements_v1.json")
    if target_path.is_file():
        return json.loads(target_path.read_text(encoding="utf-8"))
    return {
        "globalSafety": {
            "requiredSignalCodes": [
                "ALTERED_CONSCIOUSNESS", "SEIZURE", "SEVERE_BREATHING_DIFFICULTY",
                "CYANOSIS", "SELF_HARM_IDEATION", "SELF_HARM_INTENT_OR_PLAN",
                "HARM_TO_BABY_IDEATION", "CANNOT_ENSURE_OWN_SAFETY"
            ]
        },
        "byStage": {
            "PRECONCEPTION": {"contextFields": ["stage"], "greenEligibilityFields": ["stage", "possible_pregnancy"]},
            "POSSIBLE_PREGNANCY": {"contextFields": ["stage", "possible_pregnancy"], "greenEligibilityFields": ["stage", "possible_pregnancy", "last_menstrual_period_or_test"]},
            "PREGNANCY": {"contextFields": ["stage", "gestational_week"], "greenEligibilityFields": ["stage", "gestational_week", "pain_severity", "bleeding_amount"]},
            "POSTPARTUM": {"contextFields": ["stage", "postpartum_day"], "greenEligibilityFields": ["stage", "postpartum_day", "delivery_method", "bleeding_amount"]},
        }
    }


def sorted_by_precedence(rules: Sequence[Rule]) -> tuple[Rule, ...]:
    """Explicit, file-order-independent ordering.

    Physical position in the JSON is deliberately NOT a tie-break: reordering the file
    must not change a clinical decision. Order is outcome severity, then priority
    descending, then the declared decisionOrder, then ruleId.
    """

    return tuple(
        sorted(
            rules,
            key=lambda rule: (
                _OUTCOME_SEVERITY.get(rule.outcome, 99),
                -rule.priority,
                rule.decision_order,
                rule.rule_id,
            ),
        )
    )

