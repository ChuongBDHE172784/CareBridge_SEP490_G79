"""Scope and coverage answer different questions; these tests pin the difference.

The failure this layer exists to stop: a pregnant user says "I have diarrhoea", scope correctly
says IN_SCOPE because pregnancy is CareBridge's domain, and the question planner then asks about
vaginal bleeding because it had no way to know no rule reads a diarrhoea signal.
"""

from __future__ import annotations

from app.context import CareStage, IntentType, TargetEntity
from app.rules.evaluator import ScopeStatus
from app.triage_v2.coverage_resolver import (
    CoverageStatus, clinical_questions_allowed, coverage_resolver,
)


def _state(**overrides):
    base = {
        "triageOutcome": None,
        "scopeStatus": ScopeStatus.IN_SCOPE,
        "stage": CareStage.PREGNANCY,
        "targetEntity": TargetEntity.MOTHER,
        "intent": IntentType.SYMPTOM_TRIAGE,
        "signals": {},
    }
    base.update(overrides)
    return base


def _present(code):
    return {code: {"presence": "PRESENT", "temporalStatus": "CURRENT"}}


# --------------------------------------------------------------- coverage classification

def test_a_signal_the_ruleset_reads_is_supported():
    result = coverage_resolver(_state(signals=_present("HEAVY_VAGINAL_BLEEDING")))

    assert result["coverageStatus"] is CoverageStatus.SUPPORTED
    assert result["supportedSymptomCodes"] == ["HEAVY_VAGINAL_BLEEDING"]
    assert result["blocksClinicalQuestionPlanner"] is False


def test_nothing_reported_yet_is_unknown_not_unsupported():
    """"We have not been told anything" is not "we cannot help" — the conversation continues."""

    result = coverage_resolver(_state(signals={}))

    assert result["coverageStatus"] is CoverageStatus.UNKNOWN
    assert result["blocksClinicalQuestionPlanner"] is False


def test_a_stage_with_no_rules_is_unsupported():
    """Paediatric stages have no rule in the current ruleset, so nothing there is assessable."""

    result = coverage_resolver(_state(stage=CareStage.INFANT_0_12M, targetEntity=TargetEntity.BABY))

    assert result["coverageStatus"] is CoverageStatus.UNSUPPORTED
    assert result["blocksClinicalQuestionPlanner"] is True
    assert result["blocksGreen"] is True


def test_confirmed_out_of_scope_blocks_the_clinical_catalogue():
    result = coverage_resolver(_state(scopeStatus=ScopeStatus.CONFIRMED_OUT_OF_SCOPE))

    assert result["blocksClinicalQuestionPlanner"] is True


def test_conflicted_context_never_reaches_the_clinical_catalogue():
    result = coverage_resolver(_state(scopeStatus=ScopeStatus.CONFLICTED))

    assert result["coverageStatus"] is CoverageStatus.CONFLICTED
    assert result["blocksClinicalQuestionPlanner"] is True


def test_a_decided_red_is_never_softened_by_coverage():
    """Coverage must not be able to walk back an emergency the safety gate already decided."""

    result = coverage_resolver(_state(triageOutcome="RED",
                                      scopeStatus=ScopeStatus.CONFIRMED_OUT_OF_SCOPE))

    assert result["blocksClinicalQuestionPlanner"] is False
    assert result["blocksGreen"] is False


def test_partial_coverage_keeps_asking_but_still_blocks_green():
    """Half-assessable is not reassuring: GREEN stays blocked while part is outside the rules."""

    signals = {**_present("HEAVY_VAGINAL_BLEEDING"), **_present("SEVERE_BREATHING_DIFFICULTY")}
    signals["MADE_UP_UNCOVERED_SIGNAL"] = {"presence": "PRESENT", "temporalStatus": "CURRENT"}
    result = coverage_resolver(_state(signals=signals))

    assert result["coverageStatus"] is CoverageStatus.PARTIALLY_SUPPORTED
    assert "MADE_UP_UNCOVERED_SIGNAL" in result["unsupportedSymptomCodes"]
    assert result["blocksClinicalQuestionPlanner"] is False
    assert result["blocksGreen"] is True


def test_a_complaint_no_rule_reads_is_unsupported_and_blocks_planning():
    """"Pregnant with diarrhoea": in scope, but no rule reads it, so no obstetric interrogation."""

    result = coverage_resolver(_state(signals={
        "DIARRHOEA_NOT_IN_RULESET": {"presence": "PRESENT", "temporalStatus": "CURRENT"},
    }))

    assert result["coverageStatus"] is CoverageStatus.UNSUPPORTED
    assert result["blocksClinicalQuestionPlanner"] is True
    assert result["coverageLimitations"]


def test_an_absent_signal_is_not_treated_as_reported():
    """Only what the user affirmed counts as a complaint to be covered."""

    result = coverage_resolver(_state(signals={
        "HEAVY_VAGINAL_BLEEDING": {"presence": "ABSENT", "temporalStatus": "CURRENT"},
    }))

    assert result["coverageStatus"] is CoverageStatus.UNKNOWN


# --------------------------------------------------------------- routing gate

def test_unsupported_coverage_refuses_the_clinical_catalogue():
    allowed, reason = clinical_questions_allowed(
        _state(blocksClinicalQuestionPlanner=True, coverageStatus=CoverageStatus.UNSUPPORTED)
    )

    assert allowed is False
    assert reason == "COVERAGE_UNSUPPORTED"


def test_information_and_source_intents_do_not_get_clinical_questions():
    for intent in (IntentType.GENERAL_HEALTH_INFORMATION, IntentType.SOURCE_LOOKUP):
        allowed, reason = clinical_questions_allowed(_state(intent=intent))
        assert allowed is False, intent
        assert reason == f"INTENT_{intent.value}"


def test_an_unresolved_target_is_left_to_the_catalogue_filter():
    """Blocking here would stop the clarification round that resolves the target."""

    allowed, _ = clinical_questions_allowed(_state(targetEntity=TargetEntity.UNKNOWN))

    assert allowed is True


def test_supported_symptom_triage_is_allowed_through():
    allowed, reason = clinical_questions_allowed(_state())

    assert allowed is True
    assert reason is None
