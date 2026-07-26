import pytest

from app import main as main_module
from app.graph import run_triage
from app.main import continue_intake, start_intake
from app.schemas import ChildTriageRequest, IntakeContinueRequest, IntakeStartRequest


MATERNAL_STAGES = ("PRECONCEPTION", "PREGNANCY")


def maternal_request(stage: str, **overrides) -> ChildTriageRequest:
    data = {
        "stage": stage,
        "symptomList": [],
        "duration": "1 giờ",
        "breathingStatus": "bình thường",
        "consciousnessStatus": "tỉnh táo",
        "seizure": False,
        "parentFreeText": "Tôi hơi mệt",
    }
    data.update(overrides)
    return ChildTriageRequest(**data)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    ("overrides", "rule_suffix"),
    [
        ({"breathingStatus": "khó thở"}, "BREATHING_DISTRESS"),
        ({"breathingStatus": "tím tái"}, "CYANOSIS"),
        ({"seizure": True}, "SEIZURE"),
        ({"consciousnessStatus": "ngất xỉu"}, "ALTERED_CONSCIOUSNESS"),
        ({"parentFreeText": "Tôi đang chảy máu nhiều"}, "HEAVY_BLEEDING"),
        ({"parentFreeText": "Tôi sẽ tự sát ngay bây giờ"}, "SELF_HARM"),
        ({"parentFreeText": "I have difficulty breathing"}, "BREATHING_DISTRESS"),
        ({"parentFreeText": "I am suicidal and may harm myself now"}, "SELF_HARM"),
    ],
)
def test_approved_universal_signs_are_deterministic_red(stage, overrides, rule_suffix):
    response = run_triage(
        maternal_request(stage, **overrides),
        deterministic_only=True,
    )

    assert response.stage == stage
    assert response.riskLevel == "RED"
    assert response.emergencyActionRequired is True
    assert f"RED_{stage}_{rule_suffix}" in response.matchedRules
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert response.assistantFallbackUsed is True


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "canonical_code",
    ["difficulty_breathing", "cyanosis", "seizure", "lethargy", "difficult_to_wake"],
)
def test_canonical_universal_signs_are_red_for_each_maternal_stage(
    stage, canonical_code
):
    response = run_triage(
        maternal_request(
            stage,
            symptomList=[canonical_code],
            parentFreeText=None,
        ),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_universal_red_precedes_every_gemini_operation(stage, monkeypatch):
    calls: list[str] = []

    class ExplodingGeminiClient:
        reachable = True

        def __getattr__(self, name):
            def fail(**_kwargs):
                calls.append(name)
                raise AssertionError(f"Gemini must not run before deterministic RED: {name}")

            return fail

    monkeypatch.setattr(main_module, "get_gemini_client", ExplodingGeminiClient)

    response = start_intake(IntakeStartRequest(
        currentIntake=maternal_request(
            stage,
            parentFreeText="Tôi khó thở và đang chảy máu nhiều",
        )
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert response.assistantProvider == "DETERMINISTIC_FALLBACK"
    assert calls == []


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không khó thở và không tím tái",
        "Tôi không co giật hay ngất xỉu",
        "Tôi không bị chảy máu nhiều",
        "Tôi không muốn tự hại và không có ý định tự sát",
        "I do not have difficulty breathing and I have no cyanosis",
        "I am not suicidal and do not intend self-harm",
        "I don't have difficulty breathing",
        "She doesn't have heavy bleeding",
        "She isn't unconscious",
        "She hasn't had a seizure",
        "I won't kill myself",
        "I don’t have blue lips",
    ],
)
def test_negated_universal_signs_do_not_trigger_red(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không khó thở và chảy máu nhiều",
        "I have no breathing difficulty and heavy bleeding",
    ],
)
def test_affirmed_sign_after_conjunction_is_not_suppressed_by_earlier_negation(
    stage, parent_free_text
):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert f"RED_{stage}_HEAVY_BLEEDING" in response.matchedRules
    assert f"RED_{stage}_BREATHING_DISTRESS" not in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không khó thở và không chảy máu nhiều",
        "I have no breathing difficulty and no heavy bleeding",
    ],
)
def test_each_negated_sign_across_conjunction_remains_non_red(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    ("parent_free_text", "rule_suffix"),
    [
        ("I cannot breathe", "BREATHING_DISTRESS"),
        ("I can't breathe", "BREATHING_DISTRESS"),
        ("My blue lips are getting worse", "CYANOSIS"),
        ("I have altered consciousness", "ALTERED_CONSCIOUSNESS"),
        ("I am difficult to wake", "ALTERED_CONSCIOUSNESS"),
        ("I am soaking pad after pad", "HEAVY_BLEEDING"),
        ("I have a hemorrhage", "HEAVY_BLEEDING"),
        ("I have a haemorrhage", "HEAVY_BLEEDING"),
        ("I want to die", "SELF_HARM"),
    ],
)
def test_python_recognizes_the_java_approved_maternal_synonyms(
    stage, parent_free_text, rule_suffix
):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert f"RED_{stage}_{rule_suffix}" in response.matchedRules


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "I did not say I cannot breathe",
        "I do not have blue lips",
        "I do not have altered consciousness",
        "I am not difficult to wake",
        "I am not soaking pad after pad",
        "I do not have a haemorrhage",
        "I do not want to die",
        "Patient denies blue lips and denies heavy bleeding",
    ],
)
def test_negated_java_approved_synonyms_do_not_trigger_red(stage, parent_free_text):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_java_breathing_difficulty_synonym_has_exact_single_rule(stage):
    response = run_triage(
        maternal_request(stage, parentFreeText="I have breathing difficulty"),
        deterministic_only=True,
    )

    assert response.riskLevel == "RED"
    assert response.redFlags == ["Khó thở"]
    assert response.matchedRules == [f"RED_{stage}_BREATHING_DISTRESS"]


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_negated_java_breathing_difficulty_synonym_is_not_red(stage):
    response = run_triage(
        maternal_request(stage, parentFreeText="I do not have breathing difficulty"),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Tôi không không thở được",
        "I am not unable to breathe",
    ],
)
def test_external_negation_suppresses_an_internal_negative_danger_phrase(
    stage, parent_free_text
):
    response = run_triage(
        maternal_request(stage, parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel != "RED"
    assert not any(rule.startswith(f"RED_{stage}_") for rule in response.matchedRules)


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_java_breathing_difficulty_synonym_precedes_every_gemini_operation(
    stage, monkeypatch
):
    calls: list[str] = []

    class ExplodingGeminiClient:
        reachable = True

        def __getattr__(self, name):
            def fail(**_kwargs):
                calls.append(name)
                raise AssertionError(f"Gemini must not run before deterministic RED: {name}")

            return fail

    monkeypatch.setattr(main_module, "get_gemini_client", ExplodingGeminiClient)

    response = start_intake(IntakeStartRequest(
        currentIntake=maternal_request(stage, parentFreeText="I have breathing difficulty")
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert response.triageResult.matchedRules == [f"RED_{stage}_BREATHING_DISTRESS"]
    assert calls == []


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
@pytest.mark.parametrize(
    "parent_free_text",
    [
        "I cannot breathe",
        "My lips are blue lips",
        "I have altered consciousness",
        "I am soaking pad after pad",
        "I want to die",
    ],
)
def test_java_approved_synonyms_also_precede_every_gemini_operation(
    stage, parent_free_text, monkeypatch
):
    calls: list[str] = []

    class ExplodingGeminiClient:
        reachable = True

        def __getattr__(self, name):
            def fail(**_kwargs):
                calls.append(name)
                raise AssertionError(f"Gemini must not run before deterministic RED: {name}")

            return fail

    monkeypatch.setattr(main_module, "get_gemini_client", ExplodingGeminiClient)

    response = start_intake(IntakeStartRequest(
        currentIntake=maternal_request(stage, parentFreeText=parent_free_text)
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert calls == []


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_non_danger_maternal_input_asks_stage_appropriate_questions(stage, monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = start_intake(IntakeStartRequest(currentIntake=maternal_request(
        stage,
        duration=None,
        parentFreeText="Tôi hơi mệt",
    )))

    assert response.status == "ASK_MORE"
    assert response.stage == stage
    assert {question.questionKey for question in response.questions} <= {
        "parentFreeText",
        "duration",
    }
    assert all("trẻ" not in question.text.lower() for question in response.questions)
    assert response.triageResult is None


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_complete_non_danger_maternal_input_terminalizes_yellow_at_limit(
    stage, monkeypatch
):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = continue_intake(IntakeContinueRequest(
        intakeSessionId=f"{stage.lower()}-round-limit",
        currentIntake=maternal_request(stage),
        newAnswers={},
        round=3,
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.stage == stage
    assert response.questions == []
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "YELLOW"
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules
    assert f"{stage}_RULES_NEED_CLINICAL_REVIEW" in response.triageResult.matchedRules
    expected_context = {
        "PRECONCEPTION": "trước khi mang thai",
        "PREGNANCY": "trong thai kỳ",
    }[stage]
    assert expected_context in response.triageResult.summary
    assert "trẻ" not in response.triageResult.summary.lower()
    assert "trẻ" not in response.triageResult.recommendedAction.lower()


@pytest.mark.parametrize("stage", MATERNAL_STAGES)
def test_incomplete_non_danger_maternal_input_terminalizes_yellow_at_limit(
    stage, monkeypatch
):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)

    response = continue_intake(IntakeContinueRequest(
        intakeSessionId=f"{stage.lower()}-incomplete-round-limit",
        currentIntake=maternal_request(stage, duration=None),
        newAnswers={},
        round=3,
    ))

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "YELLOW"
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules


@pytest.mark.parametrize(
    "parent_free_text",
    [
        "Thai giảm cử động",
        "Tôi đau bụng dữ dội",
        "Tôi đau đầu dữ dội và hoa mắt",
    ],
)
def test_unsigned_pregnancy_specific_signs_remain_inactive(parent_free_text):
    response = run_triage(
        maternal_request("PREGNANCY", parentFreeText=parent_free_text),
        deterministic_only=True,
    )

    assert response.riskLevel == "NEED_MORE_INFO"
    assert response.emergencyActionRequired is False
    assert response.matchedRules == ["PREGNANCY_RULES_NEED_CLINICAL_REVIEW"]
