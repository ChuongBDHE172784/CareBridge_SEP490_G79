"""P1-T9 — shared context parity vectors.

Java asserts the same file. These are the Phase 1 gate scenarios plus the accent-collision
regressions found while building the resolvers.
"""

from __future__ import annotations

import json
from pathlib import Path

import pytest

from app.context.complaint_taxonomy import classify_complaint
from app.context.enums import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.context.intent_resolver import resolve_intent
from app.context.stage_resolver import resolve_context_status, resolve_stage
from app.context.target_entity_resolver import resolve_target_entity
from app.questions.catalog_filter import FilterContext, eligible_questions

VECTORS_PATH = Path(__file__).parent / "data" / "context_parity_vectors_v1.json"


@pytest.fixture(scope="module")
def vectors() -> list[dict]:
    return json.loads(VECTORS_PATH.read_text(encoding="utf-8"))["vectors"]


def _evaluate(payload: dict) -> dict:
    message = payload.get("message")
    target = resolve_target_entity(latest_user_message=message)
    intent = resolve_intent(
        latest_user_message=message,
        submitted_option_codes=payload.get("optionCodes"),
    )
    explicit = payload.get("explicitStage")
    stage = resolve_stage(
        entity=target.entity,
        explicit_stage=CareStage(explicit) if explicit else None,
        legacy_stage_name=payload.get("legacyStage"),
        baby_age_months=payload.get("babyAgeMonths"),
    )
    status, _ = resolve_context_status(
        entity=target.entity, stage=stage.stage, intent=intent.intent,
        extra_conflicts=tuple(stage.conflicts),
    )
    return {
        "targetEntity": target.entity,
        "intent": intent.intent,
        "stage": stage.stage,
        "contextStatus": status,
        "mayProduceTriageOutcome": intent.may_produce_triage_outcome,
        "complaintCategory": classify_complaint(message).category_id,
    }


def test_every_vector_is_asserted(vectors):
    assert len(vectors) == 14


@pytest.mark.parametrize("index", range(14))
def test_context_parity_vector(vectors, index):
    vector = vectors[index]
    actual = _evaluate(vector["input"])
    expected = vector["expected"]
    label = f"{vector['id']}: {vector['description']}"

    if "targetEntity" in expected:
        assert actual["targetEntity"] is TargetEntity(expected["targetEntity"]), label
    if "intent" in expected:
        assert actual["intent"] is IntentType(expected["intent"]), label
    if "stage" in expected:
        assert actual["stage"] is CareStage(expected["stage"]), label
    if "contextStatus" in expected:
        assert actual["contextStatus"] is ContextResolutionStatus(expected["contextStatus"]), label
    if "mayProduceTriageOutcome" in expected:
        assert actual["mayProduceTriageOutcome"] is expected["mayProduceTriageOutcome"], label
    if "complaintCategory" in expected:
        assert actual["complaintCategory"] == expected["complaintCategory"], label

    if expected.get("onlyClarificationQuestions"):
        questions = eligible_questions(FilterContext(
            target_entity=actual["targetEntity"],
            stage=actual["stage"],
            intent=actual["intent"],
            context_status=actual["contextStatus"],
            missing_fields=frozenset({"bleeding_amount", "gestational_week"}),
            missing_signals=frozenset({"VAGINAL_BLEEDING"}),
        ))
        assert questions, label
        for question in questions:
            assert (question.is_target_clarification
                    or question.is_intent_clarification
                    or question.is_stage_clarification), f"{label} -> {question.question_id}"
