from fastapi import FastAPI

from app.graph import run_triage
from app.intake_question_engine import (
    ask_followup_questions,
    has_red_flag,
    merge_answers,
    new_session_id,
    reached_question_limit,
)
from app.schemas import (
    ChildTriageRequest,
    ChildTriageResponse,
    IntakeContinueRequest,
    IntakeFlowResponse,
    IntakeStartRequest,
)

app = FastAPI(title="CareBridge AI Triage Service", version="0.1.0")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/triage/child", response_model=ChildTriageResponse)
def triage_child(request: ChildTriageRequest) -> ChildTriageResponse:
    return run_triage(request)


@app.post("/triage/intake/start", response_model=IntakeFlowResponse)
def start_intake(request: IntakeStartRequest) -> IntakeFlowResponse:
    intake = request.currentIntake
    if request.initialText and not intake.parentFreeText:
        intake = intake.model_copy(update={
            "parentFreeText": request.initialText,
            "symptomList": intake.symptomList or [request.initialText],
        })
    return _build_intake_response(
        intake_session_id=new_session_id(),
        intake=intake,
        round_number=1,
    )


@app.post("/triage/intake/continue", response_model=IntakeFlowResponse)
def continue_intake(request: IntakeContinueRequest) -> IntakeFlowResponse:
    merged = merge_answers(request.currentIntake, request.newAnswers)
    return _build_intake_response(
        intake_session_id=request.intakeSessionId,
        intake=merged,
        round_number=request.round,
    )


def _build_intake_response(
    *,
    intake_session_id: str,
    intake: ChildTriageRequest,
    round_number: int,
) -> IntakeFlowResponse:
    questions = ask_followup_questions(intake)
    red_flag = has_red_flag(intake)
    if questions and not red_flag and not reached_question_limit(round_number):
        return IntakeFlowResponse(
            status="ASK_MORE",
            intakeSessionId=intake_session_id,
            mergedIntake=intake,
            assistantMessage="CareBridge can them mot vai thong tin de phan loai rui ro an toan hon.",
            questions=questions,
            round=round_number + 1,
            triageResult=None,
        )

    force_cautious = bool(questions and not red_flag and reached_question_limit(round_number))
    triage_result = run_triage(
        intake,
        force_cautious_yellow=force_cautious,
        forced_warning=(
            "Thong tin chua day du, ket qua duoc phan loai than trong."
            if force_cautious else None
        ),
    )
    return IntakeFlowResponse(
        status="TRIAGE_COMPLETE",
        intakeSessionId=intake_session_id,
        mergedIntake=intake,
        assistantMessage="CareBridge da hoan tat phan loai rui ro ban dau.",
        questions=[],
        round=round_number,
        triageResult=triage_result,
    )
