import pytest
from fastapi import HTTPException

from app.main import continue_intake, start_intake
from app.schemas import ChildTriageRequest, IntakeContinueRequest, IntakeStartRequest


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
