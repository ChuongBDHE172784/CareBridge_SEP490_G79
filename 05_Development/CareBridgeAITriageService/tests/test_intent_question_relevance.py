import pytest
from fastapi import HTTPException

from app.main import continue_intake, start_intake
from app.schemas import ChildTriageRequest, IntakeContinueRequest, IntakeStartRequest
from app.symptom_normalizer import normalize_symptoms


def test_pregnancy_diarrhea_keeps_maternal_subject_and_asks_relevant_fact():
    response = start_intake(IntakeStartRequest(
        stage="PREGNANCY",
        initialText="Tôi tiêu chảy",
        currentIntake=ChildTriageRequest(stage="PREGNANCY"),
    ))

    assert response.stage == "PREGNANCY"
    assert response.intent is not None and response.intent.subject == "MOTHER"
    assert [question.questionKey for question in response.questions][:2] == ["duration", "hydrationStatus"]
    assert all("Bé" not in question.text for question in response.questions)


def test_infant_diarrhea_asks_diarrhea_facts_before_unrelated_screening():
    response = start_intake(IntakeStartRequest(
        stage="INFANT",
        initialText="Bé tiêu chảy",
        currentIntake=ChildTriageRequest(stage="INFANT"),
    ))

    assert [question.questionKey for question in response.questions] == [
        "duration", "dehydrationSigns", "vomiting",
    ]


def test_asked_question_is_not_repeated_on_continue():
    start = start_intake(IntakeStartRequest(
        stage="INFANT",
        initialText="Bé tiêu chảy",
        currentIntake=ChildTriageRequest(stage="INFANT"),
    ))
    next_response = continue_intake(IntakeContinueRequest(
        stage="INFANT",
        intakeSessionId="session-1",
        currentIntake=start.mergedIntake,
        newAnswers={"duration": "1-3 ngày"},
        askedQuestionKeys=start.askedQuestionKeys,
        round=start.round,
    ))
    assert "duration" not in [question.questionKey for question in next_response.questions]


def test_unanswered_relevant_questions_are_reissued_on_continue():
    start = start_intake(IntakeStartRequest(
        stage="INFANT",
        initialText="Bé tiêu chảy",
        currentIntake=ChildTriageRequest(stage="INFANT"),
    ))
    next_response = continue_intake(IntakeContinueRequest(
        stage="INFANT",
        intakeSessionId="session-unanswered",
        currentIntake=start.mergedIntake,
        newAnswers={"duration": "1-3 ngày"},
        askedQuestionKeys=start.askedQuestionKeys,
        round=start.round,
    ))
    keys = [question.questionKey for question in next_response.questions]
    assert "dehydrationSigns" in keys
    assert "vomiting" in keys


def test_unknown_text_requests_one_symptom_clarification_not_duration():
    response = start_intake(IntakeStartRequest(
        stage="PREGNANCY",
        initialText="Tôi thấy không ổn",
        currentIntake=ChildTriageRequest(stage="PREGNANCY"),
    ))
    assert [question.questionKey for question in response.questions] == ["parentFreeText"]


def test_missing_or_mismatched_stage_fails_closed():
    with pytest.raises(HTTPException) as mismatch:
        start_intake(IntakeStartRequest(
            stage="PREGNANCY",
            currentIntake=ChildTriageRequest(stage="INFANT"),
        ))
    assert mismatch.value.status_code == 422


@pytest.mark.parametrize(
    ("stage", "text", "family", "expected_keys"),
    [
        ("PREGNANCY", "Tôi đau bụng", "ABDOMINAL", ["abdominalPainPattern", "painSeverity", "duration"]),
        ("INFANT", "Bé đau bụng", "ABDOMINAL", ["painSeverity", "duration", "vomiting"]),
        ("PREGNANCY", "Tôi tiểu buốt", "URINARY", ["urinarySymptoms", "duration", "temperatureC"]),
        ("INFANT", "Bé đau tai", "EAR", ["painSeverity", "duration", "temperatureC"]),
        ("PREGNANCY", "Tôi đau đầu", "HEADACHE", ["painSeverity", "duration", "temperatureC"]),
        ("INFANT", "Bé bị ngứa", "RASH", ["duration", "rash", "temperatureC"]),
    ],
)
def test_common_symptoms_select_a_named_relevant_family(stage, text, family, expected_keys):
    response = start_intake(IntakeStartRequest(
        stage=stage,
        initialText=text,
        currentIntake=ChildTriageRequest(stage=stage),
    ))

    assert response.intent is not None
    assert family in response.intent.symptomFamilies
    assert [question.questionKey for question in response.questions] == expected_keys


def test_mixed_common_gastrointestinal_symptoms_preserve_both_families_and_dedupe_questions():
    response = start_intake(IntakeStartRequest(
        stage="PREGNANCY",
        initialText="Tôi đau bụng và tiêu chảy",
        currentIntake=ChildTriageRequest(stage="PREGNANCY"),
    ))

    assert response.intent is not None
    assert {"ABDOMINAL", "DIARRHEA"} <= set(response.intent.symptomFamilies)
    keys = [question.questionKey for question in response.questions]
    # Multi-family case: abdominalPainPattern is deliberately NOT prioritised here (see
    # _relevant_missing_keys) so DIARRHEA's own follow-up isn't crowded out of the round budget.
    assert keys == ["painSeverity", "duration", "vomiting"]
    assert len(keys) == len(set(keys))


def test_mixed_families_keep_the_second_family_available_after_first_answers():
    start = start_intake(IntakeStartRequest(
        stage="PREGNANCY",
        initialText="Tôi đau bụng và tiểu buốt",
        currentIntake=ChildTriageRequest(stage="PREGNANCY"),
    ))
    next_response = continue_intake(IntakeContinueRequest(
        stage="PREGNANCY",
        intakeSessionId="mixed-families",
        currentIntake=start.mergedIntake,
        newAnswers={"painSeverity": "Nhẹ", "duration": "1-3 ngày", "vomiting": "Không"},
        askedQuestionKeys=start.askedQuestionKeys,
        round=start.round,
    ))

    assert next_response.intent is not None
    assert {"ABDOMINAL", "URINARY"} <= set(next_response.intent.symptomFamilies)
    assert "urinarySymptoms" in [question.questionKey for question in next_response.questions]


def test_common_symptom_normalization_handles_uppercase_and_negation():
    assert "abdominal_pain" in normalize_symptoms(ChildTriageRequest(parentFreeText="TÔI ĐAU BỤNG"))
    assert "headache" not in normalize_symptoms(ChildTriageRequest(parentFreeText="Tôi không đau đầu"))


@pytest.mark.parametrize("stage", ["PRECONCEPTION", "POSTPARTUM"])
def test_abdominal_pain_pattern_question_is_pregnancy_only(stage):
    # CB-TRIAGE-MATQ-IMP-001: continuous-vs-labor-pattern framing is only meaningful during
    # PREGNANCY; PRECONCEPTION/POSTPARTUM abdominal pain keeps the original question set.
    response = start_intake(IntakeStartRequest(
        stage=stage,
        initialText="Tôi đau bụng",
        currentIntake=ChildTriageRequest(stage=stage),
    ))
    assert "abdominalPainPattern" not in [q.questionKey for q in response.questions]


def test_abdominal_pain_pattern_answer_persists_through_mergedIntake():
    start = start_intake(IntakeStartRequest(
        stage="PREGNANCY",
        initialText="Tôi đau bụng",
        currentIntake=ChildTriageRequest(stage="PREGNANCY"),
    ))
    assert "abdominalPainPattern" in [q.questionKey for q in start.questions]

    next_response = continue_intake(IntakeContinueRequest(
        stage="PREGNANCY",
        intakeSessionId="pain-pattern",
        currentIntake=start.mergedIntake,
        newAnswers={"abdominalPainPattern": "Từng cơn đều đặn"},
        askedQuestionKeys=start.askedQuestionKeys,
        round=start.round,
    ))
    assert next_response.mergedIntake.abdominalPainPattern == "Từng cơn đều đặn"
    assert "abdominalPainPattern" not in [q.questionKey for q in next_response.questions]


def test_gestational_weeks_is_descriptive_only_and_never_affects_risk():
    # CB-TRIAGE-MATQ-IMP-001 (BR-SAFETY): a trusted, server-bound gestational week must never
    # change the deterministic risk classification for otherwise-identical facts.
    without_week = ChildTriageRequest(
        stage="PREGNANCY", symptomList=["abdominal_pain"], painSeverity="Nặng",
    )
    with_week = without_week.model_copy(update={"gestationalWeeks": 32})
    from app.risk_rules import apply_red_flag_rules
    without_flags, without_rules = apply_red_flag_rules(without_week, without_week.symptomList)
    with_flags, with_rules = apply_red_flag_rules(with_week, with_week.symptomList)
    assert without_flags == with_flags
    assert without_rules == with_rules


def test_gestational_weeks_out_of_bounds_is_rejected_by_schema():
    with pytest.raises(Exception):
        ChildTriageRequest(stage="PREGNANCY", gestationalWeeks=0)
    with pytest.raises(Exception):
        ChildTriageRequest(stage="PREGNANCY", gestationalWeeks=46)
