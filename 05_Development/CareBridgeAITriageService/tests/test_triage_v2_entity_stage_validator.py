"""P2-T8 entity/stage and question-boundary tests."""

from __future__ import annotations

import pytest

from app.context import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.questions.catalog import CATALOG
from app.triage_v2.entity_stage_validator import entity_stage_validator
from app.triage_v2.state import create_initial_state


def _state():
    state = create_initial_state(
        session_id="session-123",
        state_version=1,
        request_id="request-123",
        message_id="message-123",
        latest_user_message="structured context",
    )
    state["intent"] = IntentType.SYMPTOM_TRIAGE
    state["contextResolutionStatus"] = ContextResolutionStatus.RESOLVED
    return state


@pytest.mark.parametrize(
    ("entity", "stage"),
    [(TargetEntity.BABY, CareStage.PREGNANCY), (TargetEntity.MOTHER, CareStage.INFANT_0_12M)],
)
def test_invalid_entity_stage_is_blocked_without_rewriting_pair(entity, stage):
    state = _state()
    state["targetEntity"] = entity
    state["stage"] = stage

    updates = entity_stage_validator(state)

    assert updates["contextResolutionStatus"] is ContextResolutionStatus.CONFLICTED
    assert updates["contextConflicts"]
    assert state["targetEntity"] is entity
    assert state["stage"] is stage


def test_mother_only_question_is_removed_from_baby_session():
    state = _state()
    state["targetEntity"] = TargetEntity.BABY
    state["stage"] = CareStage.INFANT_0_12M
    state["missingRequiredFields"] = ["gestational_week", "baby_age_months"]
    mother_question = next(
        question for question in CATALOG.values()
        if question.target_entities == (TargetEntity.MOTHER,) and question.resolves_fields
    )
    state["candidateQuestionIds"] = [mother_question.question_id]

    assert entity_stage_validator(state)["candidateQuestionIds"] == []


def test_baby_only_question_is_removed_from_mother_session():
    state = _state()
    state["targetEntity"] = TargetEntity.MOTHER
    state["stage"] = CareStage.PREGNANCY
    state["missingRequiredFields"] = ["baby_age_months", "gestational_week"]
    baby_question = next(
        question for question in CATALOG.values()
        if question.target_entities == (TargetEntity.BABY,) and question.resolves_fields
    )
    state["candidateQuestionIds"] = [baby_question.question_id]

    assert entity_stage_validator(state)["candidateQuestionIds"] == []


def test_unknown_target_allows_only_target_clarification():
    state = _state()
    state["contextResolutionStatus"] = ContextResolutionStatus.NEEDS_TARGET_ENTITY
    state["candidateQuestionIds"] = list(CATALOG)

    assert entity_stage_validator(state)["candidateQuestionIds"] == ["Q_CLARIFY_TARGET_ENTITY"]


@pytest.mark.parametrize(
    ("entity", "expected"),
    [
        (
            TargetEntity.MOTHER,
            ["Q_PREGNANCY_TEST", "Q_GESTATIONAL_WEEK", "Q_POSTPARTUM_DAY"],
        ),
        (TargetEntity.BABY, ["Q_BABY_AGE_MONTHS"]),
    ],
)
def test_missing_stage_uses_only_fixed_entity_specific_context_questions(entity, expected):
    state = _state()
    state["targetEntity"] = entity
    state["stage"] = CareStage.UNKNOWN
    state["contextResolutionStatus"] = ContextResolutionStatus.NEEDS_STAGE

    assert entity_stage_validator(state)["candidateQuestionIds"] == expected


def test_stopped_red_clears_all_questions():
    state = _state()
    state.update(
        triageOutcome="RED",
        requiredAction="IMMEDIATE",
        stopConversation=True,
        candidateQuestionIds=["Q_CLARIFY_TARGET_ENTITY"],
        plannedQuestionIds=["Q_CLARIFY_TARGET_ENTITY"],
    )

    assert entity_stage_validator(state) == {"candidateQuestionIds": [], "plannedQuestionIds": []}
