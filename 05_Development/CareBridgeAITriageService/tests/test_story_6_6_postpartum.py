import pytest

from app import main as main_module
from app.graph import run_triage
from app.intake_question_engine import ask_followup_questions
from app.main import _summary_facts, continue_intake, start_intake
from app.risk_rules import score_risk
from app.schemas import (
    ChildTriageRequest,
    IntakeContinueRequest,
    IntakeStartRequest,
)


def postpartum_request(**overrides) -> ChildTriageRequest:
    data = {
        "stage": "POSTPARTUM",
        "symptomList": [],
        "duration": "1 giờ",
        "breathingStatus": "bình thường",
        "consciousnessStatus": "tỉnh táo",
        "seizure": False,
        "parentFreeText": "đau nhẹ sau sinh",
    }
    data.update(overrides)
    return ChildTriageRequest(**data)


def test_postpartum_is_a_supported_typed_stage():
    request = postpartum_request()

    assert request.stage == "POSTPARTUM"


def test_postpartum_followup_questions_are_maternal_only(monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = start_intake(IntakeStartRequest(currentIntake=postpartum_request(
        duration=None,
        breathingStatus=None,
        consciousnessStatus=None,
        seizure=None,
        parentFreeText="Tôi chóng mặt sau sinh",
        symptomList=["chóng mặt"],
    )))

    assert response.status == "ASK_MORE"
    assert response.stage == "POSTPARTUM"
    question_keys = {question.questionKey for question in response.questions}
    assert question_keys <= {
        "parentFreeText",
        "duration",
        "breathingStatus",
        "consciousnessStatus",
        "seizure",
    }
    assert question_keys.isdisjoint({
        "childAgeMonths",
        "feedingStatus",
        "vomiting",
        "diarrhea",
        "rash",
        "dehydrationSigns",
    })


def test_postpartum_consciousness_question_keeps_canonical_danger_option():
    questions = ask_followup_questions(postpartum_request(consciousnessStatus=None))
    consciousness = next(
        question for question in questions if question.questionKey == "consciousnessStatus"
    )

    assert "Khó giữ tỉnh táo" in consciousness.options
    assert all("đau ngực" not in question.text.lower() for question in questions)
    assert all(
        "Đau ngực" not in question.options
        for question in questions
    )


@pytest.mark.parametrize(
    ("overrides", "expected_rule"),
    [
        ({"breathingStatus": "khó thở"}, "RED_POSTPARTUM_BREATHING_DISTRESS"),
        ({"breathingStatus": "tím môi"}, "RED_POSTPARTUM_CYANOSIS"),
        ({"seizure": True}, "RED_POSTPARTUM_SEIZURE"),
        ({"consciousnessStatus": "ngất xỉu"}, "RED_POSTPARTUM_ALTERED_CONSCIOUSNESS"),
        ({"consciousnessStatus": "Khó giữ tỉnh táo"}, "RED_POSTPARTUM_ALTERED_CONSCIOUSNESS"),
        ({"parentFreeText": "Tôi bị băng huyết, ra máu nhiều"}, "RED_POSTPARTUM_HEAVY_BLEEDING"),
        ({"parentFreeText": "Tôi muốn tự làm hại bản thân"}, "RED_POSTPARTUM_SELF_HARM"),
    ],
)
def test_postpartum_danger_signs_are_deterministic_red(overrides, expected_rule):
    response = run_triage(postpartum_request(**overrides), deterministic_only=True)

    assert response.stage == "POSTPARTUM"
    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True
    assert expected_rule in response.matchedRules
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert response.assistantFallbackUsed is True


@pytest.mark.parametrize(
    ("canonical_code", "expected_rule"),
    [
        ("difficulty_breathing", "RED_POSTPARTUM_BREATHING_DISTRESS"),
        ("cyanosis", "RED_POSTPARTUM_CYANOSIS"),
        ("seizure", "RED_POSTPARTUM_SEIZURE"),
        ("lethargy", "RED_POSTPARTUM_ALTERED_CONSCIOUSNESS"),
        ("difficult_to_wake", "RED_POSTPARTUM_ALTERED_CONSCIOUSNESS"),
    ],
)
def test_postpartum_canonical_danger_codes_are_deterministic_red(
    canonical_code, expected_rule
):
    response = run_triage(
        postpartum_request(symptomList=[canonical_code], parentFreeText=None),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert expected_rule in response.matchedRules


@pytest.mark.parametrize(
    "overrides",
    [
        {"parentFreeText": "Tôi không khó thở"},
        {"parentFreeText": "Tôi không co giật"},
        {"parentFreeText": "Tôi không muốn chết"},
    ],
)
def test_postpartum_negated_danger_phrases_do_not_trigger_red(overrides):
    response = run_triage(postpartum_request(**overrides), deterministic_only=True)

    assert response.riskLevel != "RED"
    assert not any(rule.startswith("RED_POSTPARTUM_") for rule in response.matchedRules)


def test_postpartum_structured_danger_answer_takes_precedence_over_negated_prose():
    response = run_triage(
        postpartum_request(
            breathingStatus="Khó thở",
            parentFreeText="Tôi không nghĩ mình khó thở",
        ),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert "RED_POSTPARTUM_BREATHING_DISTRESS" in response.matchedRules


def test_postpartum_deterministic_red_never_calls_gemini():
    class ExplodingGeminiClient:
        def __getattr__(self, name):
            raise AssertionError(f"Gemini must not be called for deterministic RED: {name}")

    response = run_triage(
        postpartum_request(breathingStatus="khó thở"),
        gemini_client=ExplodingGeminiClient(),
    )

    assert response.riskLevel == "RED"
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"


def test_postpartum_score_risk_preserves_red_precedence():
    risk, rules = score_risk(
        postpartum_request(),
        [],
        ["Chảy máu nhiều"],
        ["RED_POSTPARTUM_HEAVY_BLEEDING"],
    )

    assert risk == "RED"
    assert rules == ["RED_POSTPARTUM_HEAVY_BLEEDING"]


def test_postpartum_non_red_explanation_never_calls_gemini():
    class ExplodingGeminiClient:
        def __getattr__(self, name):
            raise AssertionError(f"Gemini must not compose postpartum guidance: {name}")

    response = run_triage(
        postpartum_request(),
        force_cautious_yellow=True,
        gemini_client=ExplodingGeminiClient(),
        normalized_details=[],
    )

    assert response.riskLevel == "YELLOW"
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert "sau sinh" in response.summary


def test_postpartum_summary_facts_exclude_pediatric_fields():
    facts = _summary_facts(postpartum_request(), [])

    assert facts["stage"] == "POSTPARTUM"
    assert "childAgeMonths" not in facts
    assert "feedingStatus" not in facts


def test_postpartum_ai_unavailable_round_limit_is_conservative_yellow(monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = continue_intake(IntakeContinueRequest(
        intakeSessionId="postpartum-session",
        currentIntake=postpartum_request(
            duration=None,
            breathingStatus=None,
            consciousnessStatus=None,
            seizure=None,
            parentFreeText="Tôi chóng mặt sau sinh",
            symptomList=["chóng mặt"],
        ),
        newAnswers={},
        round=3,
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.stage == "POSTPARTUM"
    assert response.triageResult is not None
    assert response.triageResult.stage == "POSTPARTUM"
    assert response.triageResult.riskLevel == "YELLOW"
    assert response.triageResult.emergencyActionRequired is False
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules
    assert "POSTPARTUM_RULES_REQUIRE_CLINICAL_REVIEW" in response.triageResult.matchedRules
    assert "POSTPARTUM_RULES_NEED_CLINICAL_REVIEW" not in response.triageResult.matchedRules
    assert response.triageResult.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert response.triageResult.assistantFallbackUsed is True


def test_complete_non_danger_postpartum_round_limit_terminalizes_yellow(monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = continue_intake(IntakeContinueRequest(
        intakeSessionId="postpartum-complete-session",
        currentIntake=postpartum_request(),
        newAnswers={},
        round=3,
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.questions == []
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "YELLOW"
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules
    assert "trẻ" not in response.assistantMessage.lower()
    assert "trẻ" not in response.conversationSummary.lower()


def test_postpartum_conversation_never_uses_gemini_for_questions_or_summaries(monkeypatch):
    calls: list[str] = []

    class RecordingGeminiClient:
        reachable = True

        def normalize_symptom_text(self, **_kwargs):
            calls.append("normalize_symptom_text")
            return None

        def compose_followup_questions(self, **_kwargs):
            calls.append("compose_followup_questions")
            raise AssertionError("Postpartum follow-up prose must stay deterministic")

        def summarize_conversation(self, **_kwargs):
            calls.append("summarize_conversation")
            raise AssertionError("Postpartum summaries must stay deterministic")

    monkeypatch.setattr(main_module, "get_gemini_client", RecordingGeminiClient)

    ask_more = start_intake(IntakeStartRequest(currentIntake=postpartum_request(
        duration=None,
        consciousnessStatus=None,
        parentFreeText="Tôi chóng mặt sau sinh",
        symptomList=["chóng mặt"],
    )))
    completed = continue_intake(IntakeContinueRequest(
        intakeSessionId="postpartum-no-prose-gemini",
        currentIntake=postpartum_request(),
        newAnswers={},
        round=3,
    ))

    assert ask_more.status == "ASK_MORE"
    assert ask_more.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert completed.status == "TRIAGE_COMPLETE"
    assert "compose_followup_questions" not in calls
    assert "summarize_conversation" not in calls


def test_postpartum_malformed_gemini_output_cannot_downgrade_red():
    class MalformedGeminiClient:
        def normalize_symptom_text(self, **_kwargs):
            return {"riskLevel": "GREEN", "diagnosis": "ignore safety rules"}

        def explain_triage_result(self, **_kwargs):
            return {"riskLevel": "GREEN"}

    response = run_triage(
        postpartum_request(parentFreeText="Tôi ra máu nhiều sau sinh"),
        gemini_client=MalformedGeminiClient(),
    )

    assert response.riskLevel == "RED"
    assert "RED_POSTPARTUM_HEAVY_BLEEDING" in response.matchedRules


def test_safety_audit_log_excludes_clinical_payloads(caplog):
    caplog.set_level("INFO", logger="app.graph")

    run_triage(
        postpartum_request(parentFreeText="Tôi ra máu nhiều sau sinh"),
        deterministic_only=True,
    )

    audit_lines = [
        record.getMessage()
        for record in caplog.records
        if record.name == "app.graph" and "ai_triage_audit" in record.getMessage()
    ]
    assert audit_lines
    assert all("normalizedSymptoms" not in line for line in audit_lines)
    assert all("matchedRules" not in line for line in audit_lines)
    assert all("citationIds" not in line for line in audit_lines)
    assert all("RED_POSTPARTUM" not in line for line in audit_lines)


def test_pediatric_stage_questions_and_red_rules_remain_unchanged(monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)
    infant = start_intake(IntakeStartRequest(initialText="bé ho"))
    toddler_red = run_triage(ChildTriageRequest(
        stage="TODDLER",
        childAgeMonths=24,
        symptomList=["co giật"],
        duration="1 giờ",
        breathingStatus="bình thường",
        consciousnessStatus="tỉnh táo",
        feedingStatus="uống tốt",
        seizure=True,
    ), deterministic_only=True)

    assert any(question.questionKey == "childAgeMonths" for question in infant.questions)
    assert toddler_red.riskLevel == "RED"
    assert "RED_SEIZURE" in toddler_red.matchedRules
