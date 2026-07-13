from app.main import continue_intake, start_intake
from app.schemas import ChildTriageRequest, IntakeContinueRequest, IntakeStartRequest


def test_missing_age_asks_age():
    response = start_intake(IntakeStartRequest(initialText="be sot va ho"))

    assert response.status == "ASK_MORE"
    assert any(q.questionKey == "childAgeMonths" for q in response.questions)


def test_missing_breathing_status_asks_breathing():
    request = IntakeStartRequest(
        currentIntake=ChildTriageRequest(
            childAgeMonths=8,
            symptomList=["sot", "ho"],
            parentFreeText="be sot va ho",
            seizure=False,
            feedingStatus="bu uong tot",
            consciousnessStatus="tinh tao",
        )
    )
    response = start_intake(request)

    assert response.status == "ASK_MORE"
    assert any(q.questionKey == "breathingStatus" for q in response.questions)


def test_red_flag_in_first_round_completes_red():
    response = start_intake(
        IntakeStartRequest(
            currentIntake=ChildTriageRequest(
                childAgeMonths=8,
                symptomList=["kho tho"],
                breathingStatus="kho tho",
                parentFreeText="be kho tho",
            )
        )
    )

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "RED"
    assert response.triageResult.emergencyActionRequired is True


def test_continue_merges_answers_and_keeps_initial_symptoms():
    current = ChildTriageRequest(
        childAgeMonths=None,
        symptomList=["sot", "ho"],
        parentFreeText="be sot va ho",
    )
    response = continue_intake(
        IntakeContinueRequest(
            intakeSessionId="session-1",
            currentIntake=current,
            newAnswers={
                "childAgeMonths": 8,
                "breathingStatus": "tho binh thuong",
                "consciousnessStatus": "tinh tao",
            },
            round=1,
        )
    )

    assert response.mergedIntake.childAgeMonths == 8
    assert response.mergedIntake.symptomList == ["sot", "ho"]


def test_after_three_rounds_missing_data_returns_cautious_yellow():
    current = ChildTriageRequest(
        childAgeMonths=8,
        symptomList=["ho"],
        parentFreeText="be ho",
    )
    response = continue_intake(
        IntakeContinueRequest(
            intakeSessionId="session-2",
            currentIntake=current,
            newAnswers={},
            round=3,
        )
    )

    assert response.status == "TRIAGE_COMPLETE"
    assert response.triageResult is not None
    assert response.triageResult.riskLevel == "YELLOW"
    assert "YELLOW_INCOMPLETE_INFORMATION" in response.triageResult.matchedRules
    assert response.triageResult.warning == "Thong tin chua day du, ket qua duoc phan loai than trong."
