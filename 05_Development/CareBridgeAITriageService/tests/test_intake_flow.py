from app import main as main_module
from app.main import continue_intake, start_intake
from app.schemas import (
    ChildTriageRequest,
    GeminiConversationSummary,
    GeminiExplanation,
    IntakeContinueRequest,
    IntakeStartRequest,
)


def test_missing_age_asks_age():
    response = start_intake(IntakeStartRequest(initialText="be sot va ho"))

    assert response.status == "ASK_MORE"
    assert any(q.questionKey == "childAgeMonths" for q in response.questions)


def test_start_echoes_canonical_session_id_from_spring():
    response = start_intake(IntakeStartRequest(intakeSessionId="spring-session-123", initialText="be sot"))
    assert response.intakeSessionId == "spring-session-123"


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
    assert response.mergedIntake.symptomList == ["cough", "fever"]
    assert response.mergedIntake.parentFreeText is None


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
    assert response.triageResult.warning == "Thông tin chưa đầy đủ, kết quả được phân loại thận trọng."


def test_unknown_narrative_never_returns_terminal_need_more_info(monkeypatch):
    monkeypatch.setattr(main_module, "get_gemini_client", lambda: None)
    response = start_intake(IntakeStartRequest(currentIntake=ChildTriageRequest(
        childAgeMonths=8,
        symptomList=[],
        duration="1 ngày",
        feedingStatus="bú tốt",
        breathingStatus="thở bình thường",
        consciousnessStatus="tỉnh táo",
        seizure=False,
        parentFreeText="Bé có dấu hiệu zzz chưa nhận diện được",
    )))
    assert response.status == "ASK_MORE"
    assert response.triageResult is None
    assert response.questions[0].questionKey == "parentFreeText"


def test_conversation_summary_is_integrated_when_gemini_available(monkeypatch):
    class SummaryClient:
        enabled = True

        def normalize_symptom_text(self, **_kwargs):
            return None

        def compose_followup_questions(self, **_kwargs):
            return None

        def explain_triage_result(self, **_kwargs):
            return GeminiExplanation(
                summary="Bé có dấu hiệu cần theo dõi.",
                possibleConcern="Cần quan sát diễn tiến.",
                recommendedAction="Liên hệ nhân viên y tế nếu bé xấu đi.",
                evidenceExplanation="Nguồn đã được CareBridge kiểm tra.",
                disclaimer="ignored",
            )

        def summarize_conversation(self, **_kwargs):
            return GeminiConversationSummary(summary="Bé 8 tháng, ho một ngày và vẫn tỉnh táo.")

    monkeypatch.setattr(main_module, "get_gemini_client", lambda: SummaryClient())
    response = start_intake(IntakeStartRequest(currentIntake=ChildTriageRequest(
        childAgeMonths=8,
        symptomList=["ho"],
        duration="1 ngày",
        feedingStatus="bú tốt",
        breathingStatus="thở bình thường",
        consciousnessStatus="tỉnh táo",
        seizure=False,
    )))
    assert response.status == "TRIAGE_COMPLETE"
    assert response.conversationSummary == "Bé 8 tháng, ho một ngày và vẫn tỉnh táo."
