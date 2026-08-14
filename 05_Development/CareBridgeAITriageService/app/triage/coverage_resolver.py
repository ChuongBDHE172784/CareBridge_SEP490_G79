"""Rule-coverage resolution: can the current ruleset actually assess this complaint?

Scope and coverage answer two different questions and were previously conflated:

* ``ScopeStatus`` — "is this problem within what CareBridge covers at all?"
* ``CoverageStatus`` — "does the ruleset have the rules to assess *this* complaint?"

A pregnant user reporting diarrhoea is IN_SCOPE (pregnancy is squarely CareBridge's domain) yet
UNSUPPORTED (no rule reads a diarrhoea signal for a maternal stage). Without this distinction the
question planner saw IN_SCOPE, ran, and asked obstetric questions that had nothing to do with the
complaint — the failure this module exists to stop.

Coverage never decides an outcome. It gates which question catalogue may run and blocks GREEN;
RED still comes from the safety gate and the rule engine, ahead of anything here.
"""

from __future__ import annotations

from enum import Enum
from typing import Mapping

from app.context import IntentType, TargetEntity
from app.rules.condition import Presence, signal_presence
from app.rules.evaluator import ScopeStatus
from app.rules.registry import get_registry


class CoverageStatus(str, Enum):
    SUPPORTED = "SUPPORTED"
    PARTIALLY_SUPPORTED = "PARTIALLY_SUPPORTED"
    UNSUPPORTED = "UNSUPPORTED"
    UNKNOWN = "UNKNOWN"
    CONFLICTED = "CONFLICTED"


#: Reason codes are closed so the audit trail stays machine-readable.
class CoverageReason(str, Enum):
    NO_RULE_READS_REPORTED_SIGNALS = "NO_RULE_READS_REPORTED_SIGNALS"
    RULES_COVER_REPORTED_SIGNALS = "RULES_COVER_REPORTED_SIGNALS"
    SOME_REPORTED_SIGNALS_UNCOVERED = "SOME_REPORTED_SIGNALS_UNCOVERED"
    NO_SIGNALS_REPORTED_YET = "NO_SIGNALS_REPORTED_YET"
    STAGE_HAS_NO_RULES = "STAGE_HAS_NO_RULES"
    CONTEXT_CONFLICTED = "CONTEXT_CONFLICTED"
    SCOPE_CONFIRMED_OUT = "SCOPE_CONFIRMED_OUT"
    PENDING_SAFETY_SCREEN = "PENDING_SAFETY_SCREEN"


def _present_signal_codes(signals: Mapping[str, object]) -> list[str]:
    """Signal codes the user has actually affirmed this session."""

    return sorted(
        code for code in signals
        if type(code) is str and signal_presence(signals, code) is Presence.PRESENT
    )


def _rule_readable_codes(stage: str) -> set[str]:
    """Every signal some releasable rule for this stage can read."""

    registry = get_registry()
    readable: set[str] = set()
    for rule in registry.rules:
        if stage not in getattr(rule, "stages", ()):
            continue
        readable |= set(_condition_signals(getattr(rule, "condition", None)))
    return readable


def _condition_signals(node: object) -> list[str]:
    if type(node) is dict:
        found: list[str] = []
        signal = node.get("signal")
        if type(signal) is str:
            found.append(signal)
        for value in node.values():
            found.extend(_condition_signals(value))
        return found
    if type(node) is list:
        return [code for item in node for code in _condition_signals(item)]
    return []


def coverage_resolver(state: Mapping[str, object]) -> dict[str, object]:
    """Classify how well the current ruleset can assess what the user reported.

    Runs after scope and before question planning. It is deliberately conservative: an
    unrecognised complaint is UNKNOWN, never UNSUPPORTED, and never OUT_OF_SCOPE — "we have no
    rule for this" and "this is not our field" are different statements and only the second one
    may stop a conversation.
    """

    # A decided emergency outranks coverage entirely; never let this node soften a RED.
    if state.get("triageOutcome") == "RED":
        return {
            "coverageStatus": CoverageStatus.SUPPORTED,
            "coverageReasonCodes": [],
            "supportedSymptomCodes": [],
            "unsupportedSymptomCodes": [],
            "coverageLimitations": [],
            "blocksClinicalQuestionPlanner": False,
            "blocksGreen": False,
        }

    scope = state.get("scopeStatus")
    scope_value = scope.value if hasattr(scope, "value") else scope
    if scope_value == ScopeStatus.CONFLICTED.value:
        return _result(CoverageStatus.CONFLICTED, [CoverageReason.CONTEXT_CONFLICTED],
                       blocks_planner=True)
    if scope_value == ScopeStatus.CONFIRMED_OUT_OF_SCOPE.value:
        return _result(CoverageStatus.UNSUPPORTED, [CoverageReason.SCOPE_CONFIRMED_OUT],
                       blocks_planner=True)

    signals = state.get("signals") if type(state.get("signals")) is dict else {}
    reported = _present_signal_codes(signals)
    stage = state.get("stage")
    stage_value = stage.value if hasattr(stage, "value") else stage
    readable = _rule_readable_codes(str(stage_value))

    if not readable:
        # No releasable rule targets this stage at all.
        return _result(CoverageStatus.UNSUPPORTED, [CoverageReason.STAGE_HAS_NO_RULES],
                       blocks_planner=True,
                       limitations=[f"NO_RULES_FOR_STAGE:{stage_value}"])

    if not reported:
        # Nothing affirmed yet. That is not "unsupported" — we simply do not know, and the
        # clarification/safety path should keep gathering.
        return _result(CoverageStatus.UNKNOWN, [CoverageReason.NO_SIGNALS_REPORTED_YET],
                       blocks_planner=False)

    supported = [code for code in reported if code in readable]
    unsupported = [code for code in reported if code not in readable]

    if supported and not unsupported:
        return _result(CoverageStatus.SUPPORTED, [CoverageReason.RULES_COVER_REPORTED_SIGNALS],
                       supported=supported, blocks_planner=False, blocks_green=False)
    if supported and unsupported:
        # Part of what they told us is assessable. Keep planning, but GREEN stays blocked: we
        # cannot call something reassuring while part of the complaint is outside the ruleset.
        return _result(CoverageStatus.PARTIALLY_SUPPORTED,
                       [CoverageReason.SOME_REPORTED_SIGNALS_UNCOVERED],
                       supported=supported, unsupported=unsupported, blocks_planner=False)
    return _result(CoverageStatus.UNSUPPORTED, [CoverageReason.NO_RULE_READS_REPORTED_SIGNALS],
                   unsupported=unsupported, blocks_planner=True,
                   limitations=[f"NO_RULE_FOR_SIGNAL:{code}" for code in unsupported])


def _result(
    status: CoverageStatus,
    reasons: list[CoverageReason],
    *,
    supported: list[str] | None = None,
    unsupported: list[str] | None = None,
    limitations: list[str] | None = None,
    blocks_planner: bool,
    blocks_green: bool = True,
) -> dict[str, object]:
    return {
        "coverageStatus": status,
        "coverageReasonCodes": [reason.value for reason in reasons],
        "supportedSymptomCodes": supported or [],
        "unsupportedSymptomCodes": unsupported or [],
        "coverageLimitations": limitations or [],
        "blocksClinicalQuestionPlanner": blocks_planner,
        # Only fully supported coverage may leave GREEN reachable; everything else blocks it.
        "blocksGreen": blocks_green,
    }


def clinical_questions_allowed(state: Mapping[str, object]) -> tuple[bool, str | None]:
    """Whether the clinical follow-up catalogue may run, and why not when it may not.

    The clinical catalogue is not a fallback. When it is refused the caller must route to
    clarification, a coverage limitation, out-of-scope or needs-more-info — never to whichever
    clinical question happens to be closest.
    """

    if state.get("blocksClinicalQuestionPlanner") is True:
        status = state.get("coverageStatus")
        value = status.value if hasattr(status, "value") else status
        return False, f"COVERAGE_{value}"

    # An unresolved target is deliberately NOT checked here. The catalogue filter already refuses
    # every non-clarification question while the target is unknown, and clarification is exactly
    # what should run next — blocking here would stop the conversation that resolves it.
    intent = state.get("intent")
    intent_value = intent.value if hasattr(intent, "value") else intent
    if intent_value in {IntentType.GENERAL_HEALTH_INFORMATION.value,
                        IntentType.SOURCE_LOOKUP.value,
                        IntentType.OUT_OF_SCOPE_REQUEST.value}:
        return False, f"INTENT_{intent_value}"

    return True, None
