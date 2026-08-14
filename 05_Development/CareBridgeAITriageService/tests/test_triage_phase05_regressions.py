"""Phase 0.5 regressions for the seven reviewed AI Triage V2 defects."""

from __future__ import annotations

import uuid
from types import SimpleNamespace

import pytest

import app.triage.api as api
from app.config import GeminiSettings
from app.context import CareStage, ResolutionSource, TargetEntity
from app.gemini_client import GeminiClient
from app.questions.catalog import CATALOG
from app.questions.catalog_filter import FilterContext, is_eligible
from app.rules.registry import get_registry
from app.triage.extraction import ValidatedExtraction
from app.triage.graph import build_triage_graph, graph_config
from app.triage.renderer_audit import deterministic_renderer
from app.triage.state import create_initial_state


def _state(message: str, *, target: TargetEntity | None = None,
           stage: CareStage | None = None, **values):
    state = create_initial_state(
        session_id=f"phase05-{uuid.uuid4()}",
        state_version=0,
        request_id=f"request_{uuid.uuid4().hex[:16]}",
        message_id=f"message_{uuid.uuid4().hex[:16]}",
        latest_user_message=message,
    )
    if target is not None:
        state.update(
            targetEntity=target,
            targetEntitySource=ResolutionSource.EXPLICIT_SELECTED_PROFILE,
        )
    if stage is not None:
        state["stage"] = stage
    state.update(values)
    return state


def _invoke(state):
    return build_triage_graph().invoke(state, graph_config(state["sessionId"]))


@pytest.mark.parametrize(
    "state, clarification",
    [
        (_state("Em thấy khó chịu", target=TargetEntity.MOTHER, stage=CareStage.PREGNANCY,
                gestationalWeek=30), "Q_CLARIFY_INTENT"),
        (_state("Bé không ổn lắm", target=TargetEntity.BABY,
                stage=CareStage.TODDLER_12_24M, babyAgeMonths=15), "Q_CLARIFY_INTENT"),
        (_state("Em bị sốt và bé cũng bị sốt"), "Q_CLARIFY_TARGET_FIRST"),
    ],
)
def test_bug_1_inv_1_unresolved_context_keeps_global_danger_in_parallel(state, clarification):
    result = _invoke(state)

    assert result["safetyScreenStatus"] != "COMPLETE"
    assert result["stopConversation"] is False
    assert clarification in result["plannedQuestionIds"]
    assert "Q_GLOBAL_DANGER" in result["plannedQuestionIds"]


@pytest.mark.parametrize(
    "state",
    [
        _state("Tôi bị tiêu chảy", target=TargetEntity.MOTHER,
               stage=CareStage.POSTPARTUM_MOTHER, postpartumDay=4,
               signals={"DIARRHOEA": {"presence": "PRESENT"}}),
        _state("Tôi bị nôn", target=TargetEntity.MOTHER, stage=CareStage.PREGNANCY,
               gestationalWeek=20, signals={"VOMITING": {"presence": "PRESENT"}}),
    ],
)
def test_bug_2_coverage_refusal_never_rejects_global_danger(state):
    result = _invoke(state)

    assert result["coverageStatus"] == "UNSUPPORTED"
    assert result["safetyScreenStatus"] != "COMPLETE"
    assert result["stopConversation"] is False
    assert "Q_GLOBAL_DANGER" in result["plannedQuestionIds"]
    assert "Q_GLOBAL_DANGER" not in {
        item["questionId"] for item in result["rejectedQuestionIds"]
    }


def test_bug_3_red_discriminators_and_postpartum_signal_are_reachable():
    pregnancy = FilterContext(
        target_entity=TargetEntity.MOTHER,
        stage=CareStage.PREGNANCY,
        context_status="RESOLVED",
        intent="SYMPTOM_TRIAGE",
        missing_fields=frozenset({"visual_change"}),
    )
    postpartum = FilterContext(
        target_entity=TargetEntity.MOTHER,
        stage=CareStage.POSTPARTUM_MOTHER,
        context_status="RESOLVED",
        intent="SYMPTOM_TRIAGE",
        missing_fields=frozenset({"clots", "dizziness"}),
    )

    assert is_eligible(CATALOG["Q_VISUAL_CHANGE"], pregnancy)
    assert is_eligible(CATALOG["Q_CLOTS"], postpartum)
    assert is_eligible(CATALOG["Q_DIZZINESS"], postpartum)
    assert "HEAVY_POSTPARTUM_BLEEDING" in CATALOG["Q_BLEEDING_AMOUNT"].resolves_signals

    result = _invoke(_state(
        "Tôi bị đau đầu dữ dội",
        target=TargetEntity.MOTHER,
        stage=CareStage.PREGNANCY,
        gestationalWeek=33,
        signals={"SEVERE_HEADACHE": {"presence": "PRESENT"}},
    ))
    assert "Q_VISUAL_CHANGE" in result["plannedQuestionIds"]


def test_bug_4_every_question_turn_has_a_java_allowlisted_outcome():
    result = _invoke(_state("Em thấy khó chịu", target=TargetEntity.MOTHER,
                            stage=CareStage.PREGNANCY, gestationalWeek=30))

    assert result["plannedQuestionIds"]
    assert result["triageOutcome"] == "NEEDS_MORE_INFO"


def test_bug_5_inv_5_llm_observation_conflicts_instead_of_overwriting_answer(monkeypatch):
    answer = {
        "presence": "ABSENT",
        "temporalStatus": "CURRENT",
        "provenance": "QUESTION_ANSWER",
        "sourceQuestionId": "Q_VISUAL_CHANGE",
        "sourceOptionCode": "VISUAL_CHANGE_NO",
    }
    llm = {
        "presence": "PRESENT",
        "temporalStatus": "CURRENT",
        "explicitNegation": False,
        "provenance": "LLM_EXTRACTED_VALIDATED",
    }
    monkeypatch.setattr(api, "get_gemini_client", lambda: object())
    monkeypatch.setattr(
        api,
        "extract_and_validate",
        lambda text, extractor, **kwargs: ValidatedExtraction(signals={
            "VISUAL_DISTURBANCE": llm
        }, language="vi"),
    )
    monkeypatch.setattr(api, "retrieve_verified_evidence", lambda *args, **kwargs: [])
    request = api.TriageTurnRequest.model_validate({
        "sessionId": str(uuid.uuid4()),
        "stateVersion": 0,
        "expectedStateVersion": 0,
        "requestId": f"request_{uuid.uuid4().hex[:16]}",
        "messageId": f"message_{uuid.uuid4().hex[:16]}",
        "latestUserMessage": "Tôi nhìn mờ",
        "selectedTarget": "MOTHER",
        "journeyContext": {"stage": "PREGNANCY", "gestationalWeek": 33},
        "signals": {"VISUAL_DISTURBANCE": answer},
        "measurements": {},
        "answeredQuestionIds": ["Q_VISUAL_CHANGE"],
        "submittedOptionCodes": ["VISUAL_CHANGE_NO"],
        "expectedRulesetHash": get_registry().ruleset_sha256,
    })

    result = api.execute_turn(request).state
    observation = result["signals"]["VISUAL_DISTURBANCE"]

    assert observation["presence"] == "CONFLICTED"
    assert "SIGNAL_CONFLICTED:VISUAL_DISTURBANCE" in result["dataConflicts"]

    accumulated = {"VISUAL_DISTURBANCE": answer}
    for _ in range(6):
        accumulated = api._merge_observations(
            accumulated, {"VISUAL_DISTURBANCE": llm}
        )
    retained = accumulated["VISUAL_DISTURBANCE"]
    assert any(item.get("provenance") == "QUESTION_ANSWER" for item in retained)


@pytest.mark.parametrize(
    "action",
    ["ROUTE_TO_HEALTHCARE_WORKER", "STATE_VERSION_CONFLICT", "SYSTEM_UNAVAILABLE"],
)
def test_bug_6_required_action_always_has_a_renderable_response(action):
    rendered = deterministic_renderer({
        "triageOutcome": None,
        "requiredAction": action,
        "plannedQuestionIds": [],
        "stopConversation": True,
        "reasonCodes": [],
    })

    assert rendered["finalResponse"] is not None
    assert rendered["triageOutcome"] == "NEEDS_MORE_INFO"
    if action == "STATE_VERSION_CONFLICT":
        assert rendered["finalResponse"]["templateId"] == "TECHNICAL.STATE_VERSION_CONFLICT"
        assert rendered["finalResponse"]["action"] == action


def test_bug_7_v2_extraction_forwards_a_bounded_deadline():
    class Probe(GeminiClient):
        def __init__(self):
            super().__init__(GeminiSettings(True, "test-key", "gemini-test", 6.0, 1, 0.0),
                             object())
            self.deadline_seen = None

        def _generate(self, *args, deadline=None, **kwargs):
            self.deadline_seen = deadline
            return None

    probe = Probe()
    probe.extract_triage(text="Tôi bị đau", deadline=123.0)

    assert probe.deadline_seen == 123.0


# Legitimate rule-question exceptions: these questions resolve condition/triage evidence that
# the legacy requiredFields list does not repeat. Each pair is intentionally reviewable.
_INV3_ALLOWED_RULE_QUESTIONS = {
    ("PREG_RED_001", "Q_DIZZINESS"),  # supplemental instability symptom for heavy bleeding
    ("PREG_RED_002", "Q_BP_IF_KNOWN"),  # supporting pre-eclampsia measurement
    ("PED_RED_001", "Q_GLOBAL_DANGER"),  # resolves respiratory/cyanosis trigger signals
    ("PED_RED_002", "Q_GLOBAL_DANGER"),  # resolves seizure/consciousness trigger signals
    ("PED_RED_003", "Q_BABY_FEEDING"),  # resolves POOR_FEEDING trigger
    ("PED_RED_004", "Q_BABY_HYDRATION"),  # resolves dehydration trigger
    ("PED_RED_005", "Q_BABY_TEMPERATURE"),  # resolves fever band alongside trusted value
    ("PED_RED_006", "Q_BABY_TEMPERATURE"),  # resolves HIGH_FEVER trigger
    ("PED_YELLOW_001", "Q_BABY_TEMPERATURE"),  # resolves FEVER trigger
    ("PED_YELLOW_001", "Q_BABY_HYDRATION"),  # resolves dehydration branch
}


def test_inv_3_every_rule_question_resolves_its_rule_or_is_explained():
    violations = set()
    for rule in get_registry().rules:
        for question_id in rule.question_ids:
            question = CATALOG[question_id]
            resolved = set(question.resolves_fields) | set(question.resolves_signals)
            if not resolved.intersection(rule.required_fields):
                violations.add((rule.rule_id, question_id))

    assert violations == _INV3_ALLOWED_RULE_QUESTIONS


# Values supplied outside the question catalog. This is keyed by distinct field so repeated rule
# occurrences cannot hide or inflate the review surface.
_INV4_EXTERNAL_FIELD_SOURCES = {
    "temperatureC": "numeric measurement supplied by the trusted boundary",
    # Registry temporal enum required by maternal rules; no V2 producer/question was identified.
    "current_status": "declared temporal context field with an unresolved producer gap",
    "onset": "SYS_INFO_001 system-rule completeness input",
    "severity": "SYS_INFO_001 system-rule completeness input",
    "red_flags_checked": "SYS_INFO_001 system-rule safety-completeness input",
    "raw_text": "SYS_OOS_001 transport pseudo-field",
    # Known data defect: a human-readable phrase leaked into GREEN_DEFAULT_001.requiredFields.
    # It remains visible and reported; changing clinical rule data is outside this task.
    "stage-specific minimum dataset": "known GREEN_DEFAULT_001 machine-field data defect",
}


def test_inv_4_every_releasable_required_field_is_question_resolved_or_external():
    question_fields = {
        field for question in CATALOG.values() for field in question.resolves_fields
    }
    missing = {
        field
        for rule in get_registry().rules
        for field in rule.required_fields
        if field not in question_fields
    }

    assert missing == set(_INV4_EXTERNAL_FIELD_SOURCES)


def test_inv_6_every_graph_required_action_has_a_final_response():
    cases = [
        _state("Em thấy khó chịu", target=TargetEntity.MOTHER,
               stage=CareStage.PREGNANCY, gestationalWeek=30),
        _state("Tôi bị tiêu chảy", target=TargetEntity.MOTHER,
               stage=CareStage.POSTPARTUM_MOTHER, postpartumDay=4,
               signals={"DIARRHOEA": {"presence": "PRESENT"}}),
    ]
    for state in cases:
        result = _invoke(state)
        assert result["requiredAction"] is not None
        assert result["finalResponse"] is not None
