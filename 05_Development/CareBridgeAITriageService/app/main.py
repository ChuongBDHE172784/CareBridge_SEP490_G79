import time

from fastapi import Depends, FastAPI, HTTPException
from fastapi.responses import JSONResponse

from app.config import GEMINI_SETTINGS, PYTHON_SERVICE_TIMEOUT_SECONDS
from app.gemini_client import get_gemini_client
from app.graph import run_triage
from app.intake_question_engine import (
    ask_followup_questions,
    extract_triage_intent,
    has_red_flag,
    merge_answers,
    naturalize_followup_questions,
    new_session_id,
    reached_question_limit,
)
from app.schemas import (
    ChildTriageRequest,
    ChildTriageResponse,
    IntakeContinueRequest,
    IntakeFlowResponse,
    IntakeQuestion,
    IntakeStartRequest,
)
from app.risk_rules import MATERNAL_STAGES, apply_red_flag_rules
from app.symptom_normalizer import (
    normalize_symptom_details_deterministic,
    normalize_symptom_details_with_metadata,
)
from app.triage_v2.api import (
    TriageV2TurnRequest,
    TriageV2TurnResponse,
    execute_turn,
    require_internal_key,
)

app = FastAPI(title="CareBridge AI Triage Service", version="0.1.0")


@app.post(
    "/internal/triage/v2/turn",
    response_model=TriageV2TurnResponse,
    dependencies=[Depends(require_internal_key)],
    include_in_schema=False,
)
def triage_v2_turn(request: TriageV2TurnRequest) -> TriageV2TurnResponse:
    return execute_turn(request)


@app.get("/health")
def health() -> JSONResponse:
    try:
        client = get_gemini_client()
    except Exception:
        # Gemini augments explanations but is not part of the deterministic
        # safety engine's readiness contract.
        client = None
    try:
        _run_deterministic_readiness_probe()
        deterministic_ready = True
    except Exception:
        # Readiness must fail closed without leaking rules, clinical inputs, or
        # exception details into an unauthenticated operations endpoint.
        deterministic_ready = False
    payload = {
        "status": "UP" if deterministic_ready else "DOWN",
        "readiness": "READY" if deterministic_ready else "NOT_READY",
        "deterministicEngine": {
            "ready": deterministic_ready,
            "probe": "POSTPARTUM_RED_PRECEDENCE",
        },
        "geminiEnabled": GEMINI_SETTINGS.enabled,
        "geminiConfigured": bool(GEMINI_SETTINGS.api_key),
        "geminiReachable": client.reachable if client is not None else False,
        "model": GEMINI_SETTINGS.model,
    }
    return JSONResponse(status_code=200 if deterministic_ready else 503, content=payload)


def _run_deterministic_readiness_probe() -> None:
    result = run_triage(
        ChildTriageRequest(
            stage="POSTPARTUM",
            symptomList=["heavy bleeding"],
            parentFreeText="heavy bleeding",
        ),
        deterministic_only=True,
    )
    if result.riskLevel != "RED" or not result.emergencyActionRequired:
        raise RuntimeError("deterministic RED precedence probe failed")


@app.post(
    "/triage/child",
    response_model=ChildTriageResponse,
    dependencies=[Depends(require_internal_key)],
)
def triage_child(request: ChildTriageRequest) -> ChildTriageResponse:
    if request.stage is None:
        raise HTTPException(status_code=422, detail="A canonical triage stage is required")
    return run_triage(request)


@app.post(
    "/triage/intake/start",
    response_model=IntakeFlowResponse,
    dependencies=[Depends(require_internal_key)],
)
def start_intake(request: IntakeStartRequest) -> IntakeFlowResponse:
    intake = _canonical_intake(request.currentIntake, request.stage)
    if request.initialText and not intake.parentFreeText:
        intake = intake.model_copy(update={
            "parentFreeText": request.initialText,
            "symptomList": intake.symptomList or [request.initialText],
        })
    if request.healthContext and not intake.healthContext:
        # CB-TRIAGE-THMC-IMP-001: advisory-only prior context (never used by risk rules).
        intake = intake.model_copy(update={"healthContext": request.healthContext})
    return _build_intake_response(
        intake_session_id=request.intakeSessionId or new_session_id(),
        intake=intake,
        round_number=1,
        asked_question_keys=set(),
    )


@app.post(
    "/triage/intake/continue",
    response_model=IntakeFlowResponse,
    dependencies=[Depends(require_internal_key)],
)
def continue_intake(request: IntakeContinueRequest) -> IntakeFlowResponse:
    merged = merge_answers(_canonical_intake(request.currentIntake, request.stage), request.newAnswers)
    return _build_intake_response(
        intake_session_id=request.intakeSessionId,
        intake=merged,
        round_number=request.round,
        asked_question_keys=set(request.askedQuestionKeys),
    )


def _canonical_intake(
    intake: ChildTriageRequest, requested_stage: str | None
) -> ChildTriageRequest:
    """Reconcile the two wire locations without silently choosing a different subject.

    Legacy direct callers may provide only the nested stage. Production callers provide
    both values, which must agree; an absent value in both locations is rejected.
    """
    if requested_stage is not None and intake.stage is not None and requested_stage != intake.stage:
        raise HTTPException(status_code=422, detail="Conflicting triage stages")
    stage = requested_stage or intake.stage
    if stage is None:
        raise HTTPException(status_code=422, detail="A canonical triage stage is required")
    return intake.model_copy(update={"stage": stage})


def _build_intake_response(
    *,
    intake_session_id: str,
    intake: ChildTriageRequest,
    round_number: int,
    asked_question_keys: set[str],
) -> IntakeFlowResponse:
    request_deadline = time.monotonic() + PYTHON_SERVICE_TIMEOUT_SECONDS
    client = get_gemini_client()
    conversation_client = None if intake.stage == "POSTPARTUM" else client
    red_flag = has_red_flag(intake)
    if red_flag:
        normalized_details = normalize_symptom_details_deterministic(intake)
        normalization_gemini_used = False
    else:
        normalized_details, normalization_gemini_used = normalize_symptom_details_with_metadata(
            intake, client, request_deadline
        )
        normalized_red_flags, _ = apply_red_flag_rules(
            intake, [item.normalizedCode for item in normalized_details]
        )
        red_flag = bool(normalized_red_flags)
    persisted_intake = intake.model_copy(update={
        "symptomList": [item.normalizedCode for item in normalized_details],
        "parentFreeText": None,
        "stage": intake.stage,
        # CB-TRIAGE-THMC-IMP-001: context is request-scoped advisory input — never echoed
        # back through mergedIntake (the Java side re-loads it server-side per intake).
        "healthContext": [],
    })
    intent = extract_triage_intent(intake)
    questions = ask_followup_questions(intake, asked_keys=asked_question_keys)
    issued_question_keys = list(dict.fromkeys([*asked_question_keys, *(q.questionKey for q in questions)]))
    if questions and not red_flag and not reached_question_limit(round_number):
        questions, assistant_message, followup_gemini_used = naturalize_followup_questions(
            questions,
            intake=intake,
            normalized_symptoms=[item.normalizedCode for item in normalized_details],
            gemini_client=conversation_client,
            deadline=request_deadline,
        )
        conversation_summary = _conversation_summary(
            intake, [item.normalizedCode for item in normalized_details]
        )
        if conversation_client is not None:
            generated_summary = conversation_client.summarize_conversation(
                facts=_summary_facts(intake, normalized_details), deadline=request_deadline
            )
            if generated_summary is not None:
                conversation_summary = generated_summary.summary
        return IntakeFlowResponse(
            status="ASK_MORE",
            intakeSessionId=intake_session_id,
            stage=intake.stage,
            mergedIntake=persisted_intake,
            normalizedSymptomDetails=normalized_details,
            assistantMessage=assistant_message,
            questions=questions,
            round=round_number + 1,
            triageResult=None,
            assistantProvider="GEMINI" if followup_gemini_used else "DETERMINISTIC_FALLBACK",
            assistantFallbackUsed=not followup_gemini_used,
            conversationSummary=conversation_summary,
            intent=intent,
            askedQuestionKeys=issued_question_keys,
        )

    force_cautious = bool(
        not red_flag
        and (
            (
                reached_question_limit(round_number)
                and (
                    # A missing thermometer reading alone must not erase a
                    # completed respiratory assessment at the hard stop; it
                    # remains useful context, but is not a danger-sign gate.
                    any(question.questionKey != "temperatureC" for question in questions)
                    or intake.stage in MATERNAL_STAGES
                    or (
                        intake.stage in {"INFANT", "TODDLER"}
                        and not normalized_details
                    )
                )
            )
            # One explicit clarification is the most we can safely request for
            # unrecognized text.  Do not loop on the same free-text prompt.
            or (
                intent.careGoal == "CLARIFY_SYMPTOM"
                and "parentFreeText" in asked_question_keys
            )
        )
    )
    suppress_followups = bool(
        reached_question_limit(round_number)
        and questions
        and all(question.questionKey == "temperatureC" for question in questions)
    )
    triage_result = run_triage(
        intake,
        force_cautious_yellow=force_cautious,
        forced_warning=(
            "Thông tin chưa đầy đủ, kết quả được phân loại thận trọng."
            if force_cautious else None
        ),
        gemini_client=client,
        normalized_details=normalized_details,
        normalization_gemini_used=normalization_gemini_used,
        request_deadline=request_deadline,
        suppress_followups=suppress_followups,
    )
    if triage_result.riskLevel == "NEED_MORE_INFO":
        generic_questions = ask_followup_questions(intake, asked_keys=asked_question_keys)
        if not generic_questions:
            generic_questions = [IntakeQuestion(
                questionKey="parentFreeText",
                text=(
                    f"Vui lòng mô tả cụ thể dấu hiệu bạn đang gặp {_maternal_context(intake.stage)}."
                    if intake.stage in MATERNAL_STAGES
                    else "Vui lòng mô tả dấu hiệu cụ thể mà bạn quan sát được ở trẻ."
                ),
                answerType="TEXT",
            )]
        return IntakeFlowResponse(
            status="ASK_MORE",
            intakeSessionId=intake_session_id,
            stage=intake.stage,
            mergedIntake=persisted_intake,
            normalizedSymptomDetails=normalized_details,
            assistantMessage=(
                f"CareBridge cần bạn mô tả rõ hơn các dấu hiệu {_maternal_context(intake.stage)}."
                if intake.stage in MATERNAL_STAGES
                else "CareBridge cần bạn mô tả rõ hơn các dấu hiệu đang quan sát được."
            ),
            questions=generic_questions[:3],
            round=min(round_number + 1, 3),
            triageResult=None,
            assistantProvider="DETERMINISTIC_FALLBACK",
            assistantFallbackUsed=True,
            conversationSummary=_conversation_summary(
                intake, [item.normalizedCode for item in normalized_details]
            ),
            intent=intent,
            askedQuestionKeys=list(dict.fromkeys([*asked_question_keys, *(q.questionKey for q in generic_questions)])),
        )
    conversation_summary = _conversation_summary(
        intake, [item.normalizedCode for item in normalized_details]
    )
    if conversation_client is not None and triage_result.riskLevel != "RED":
        generated_summary = conversation_client.summarize_conversation(
            facts=_summary_facts(intake, normalized_details), deadline=request_deadline
        )
        if generated_summary is not None:
            conversation_summary = generated_summary.summary
    return IntakeFlowResponse(
        status="TRIAGE_COMPLETE",
        intakeSessionId=intake_session_id,
        stage=intake.stage,
        mergedIntake=persisted_intake,
        normalizedSymptomDetails=normalized_details,
        assistantMessage="CareBridge đã hoàn tất phân loại rủi ro ban đầu.",
        questions=[],
        round=round_number,
        triageResult=triage_result,
        assistantProvider=triage_result.assistantProvider,
        assistantFallbackUsed=triage_result.assistantFallbackUsed,
        conversationSummary=conversation_summary,
        intent=intent,
        askedQuestionKeys=issued_question_keys,
    )


def _summary_facts(intake: ChildTriageRequest, details: list) -> dict[str, object]:
    common = {
        "stage": intake.stage,
        "normalizedSymptoms": [item.normalizedCode for item in details],
        "duration": intake.duration,
        "temperatureC": intake.temperatureC,
        "breathingStatus": intake.breathingStatus,
        "consciousnessStatus": intake.consciousnessStatus,
    }
    if intake.stage in MATERNAL_STAGES:
        return common
    return {
        **common,
        "childAgeMonths": intake.childAgeMonths,
        "feedingStatus": intake.feedingStatus,
    }


def _conversation_summary(intake: ChildTriageRequest, symptoms: list[str]) -> str:
    if intake.stage in MATERNAL_STAGES:
        signs = ", ".join(symptoms) if symptoms else "chưa chuẩn hóa được dấu hiệu"
        duration = intake.duration or "chưa rõ thời gian"
        return (
            f"Tóm tắt intake {_maternal_context(intake.stage)}: "
            f"dấu hiệu: {signs}; thời gian: {duration}."
        )
    age = f"{intake.childAgeMonths} tháng" if intake.childAgeMonths is not None else "chưa rõ tuổi"
    signs = ", ".join(symptoms) if symptoms else "chưa chuẩn hóa được triệu chứng"
    duration = intake.duration or "chưa rõ thời gian"
    return f"Tóm tắt intake: trẻ {age}; dấu hiệu: {signs}; thời gian: {duration}."


def _maternal_context(stage: str) -> str:
    return {
        "PRECONCEPTION": "trước khi mang thai",
        "PREGNANCY": "trong thai kỳ",
        "POSTPARTUM": "sau sinh",
    }[stage]
