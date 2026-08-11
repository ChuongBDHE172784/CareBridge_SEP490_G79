"""P2-T10 question, resume, duplicate, and stale-version tests."""

from __future__ import annotations

import pytest

from app.context import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.questions.catalog import CATALOG
from app.triage_v2.input_validator import InputValidationError
from app.triage_v2.question_resume import question_planner, resume_safety_entry
from app.triage_v2.state import create_initial_state


def _state():
    state = create_initial_state(
        session_id="session-123",
        state_version=2,
        expected_state_version=2,
        request_id="request-123",
        message_id="message-123",
        latest_user_message="follow up",
    )
    state.update(
        targetEntity=TargetEntity.MOTHER,
        stage=CareStage.PREGNANCY,
        intent=IntentType.SYMPTOM_TRIAGE,
        contextResolutionStatus=ContextResolutionStatus.RESOLVED,
    )
    return state


def test_planner_returns_at_most_three_fixed_catalog_questions_and_advances_once():
    state = _state()
    state["candidateQuestionIds"] = list(CATALOG)
    state["missingRequiredFields"] = [
        field for question in CATALOG.values() for field in (*question.resolves_fields, *question.resolves_signals)
    ]

    updates = question_planner(state)

    assert 0 < len(updates["plannedQuestionIds"]) <= 3
    assert set(updates["plannedQuestionIds"]) <= set(CATALOG)
    assert updates["questionRound"] == 1


def test_target_clarification_and_global_danger_run_when_target_unknown():
    state = _state()
    state["targetEntity"] = TargetEntity.UNKNOWN
    state["contextResolutionStatus"] = ContextResolutionStatus.NEEDS_TARGET_ENTITY
    state["candidateQuestionIds"] = list(CATALOG)

    updates = question_planner(state)
    assert updates["plannedQuestionIds"] == ["Q_CLARIFY_TARGET_ENTITY", "Q_GLOBAL_DANGER"]
    assert updates["triageOutcome"] == "NEEDS_MORE_INFO"


def test_unmeasurable_measurement_pivots_and_is_not_reasked():
    state = _state()
    measurement = next(question for question in CATALOG.values() if question.measurement and question.pivot_to)
    state["candidateQuestionIds"] = [measurement.question_id, *measurement.pivot_to]
    state["missingRequiredFields"] = [*measurement.resolves_fields, *measurement.resolves_signals]
    state["signals"][measurement.question_id] = {"presence": "UNAWARE_OR_UNMEASURABLE"}

    updates = question_planner(state)

    assert measurement.question_id not in updates["plannedQuestionIds"]
    assert set(updates["plannedQuestionIds"]) <= set(measurement.pivot_to)


def test_duplicate_message_or_request_never_increments_round():
    state = _state()
    state["questionRound"] = 2
    state["candidateQuestionIds"] = ["Q_CLARIFY_TARGET_ENTITY"]
    state["processedMessageIds"] = [state["messageId"]]

    updates = question_planner(state)

    assert "questionRound" not in updates
    assert updates["plannedQuestionIds"] == []


def test_stale_version_is_a_controlled_non_clinical_conflict():
    state = _state()
    state["expectedStateVersion"] = 1

    updates = question_planner(state)

    assert updates["triageOutcome"] == "NEEDS_MORE_INFO"
    assert updates["requiredAction"] == "STATE_VERSION_CONFLICT"
    assert updates["stopConversation"] is True
    assert updates["processingErrors"][0]["code"] == "STALE_STATE_VERSION"


def test_round_three_stops_without_asking_a_fourth_round():
    state = _state()
    state["questionRound"] = 3

    updates = question_planner(state)

    assert updates["plannedQuestionIds"] == []
    assert updates["stopConversation"] is True


def test_resume_rejects_malformed_input_before_safety_evaluation():
    state = _state()
    state["latestUserMessage"] = "   "

    with pytest.raises(InputValidationError) as caught:
        resume_safety_entry(state)
    assert caught.value.field == "latestUserMessage"


def test_resume_runs_global_safety_before_question_planning():
    state = _state()
    state["signals"]["SEIZURE"] = {"presence": "PRESENT"}

    updates = resume_safety_entry(state)

    assert updates["triageOutcome"] == "RED"
    assert updates["stopConversation"] is True
    assert updates["plannedQuestionIds"] == []
