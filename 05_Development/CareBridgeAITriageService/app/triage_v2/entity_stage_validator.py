"""Hard entity/stage and question eligibility boundary for Triage V2."""

from __future__ import annotations

from typing import Mapping, TypeVar

from app.context import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.context.stage_resolver import validate_entity_stage
from app.questions.catalog_filter import FilterContext, eligible_questions

_EnumT = TypeVar("_EnumT")
_PAIR_PREFIX = "STAGE_NOT_VALID_FOR_ENTITY:"


def entity_stage_validator(state: Mapping[str, object]) -> dict[str, object]:
    """Keep invalid context visible and pass only catalog-eligible question IDs."""

    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is True:
        return {"candidateQuestionIds": [], "plannedQuestionIds": []}

    entity = _enum(state.get("targetEntity"), TargetEntity, TargetEntity.UNKNOWN)
    stage = _enum(state.get("stage"), CareStage, CareStage.UNKNOWN)
    intent = _enum(state.get("intent"), IntentType, IntentType.UNKNOWN)
    status = _enum(
        state.get("contextResolutionStatus"),
        ContextResolutionStatus,
        ContextResolutionStatus.INSUFFICIENT_CONTEXT,
    )
    conflicts = [
        item for item in state.get("contextConflicts", []) if not item.startswith(_PAIR_PREFIX)
    ]
    pair_conflicts = validate_entity_stage(entity, stage)
    conflicts.extend(pair_conflicts)
    if pair_conflicts:
        status = ContextResolutionStatus.CONFLICTED

    missing = frozenset([*state.get("missingRequiredFields", []), *state.get("unknownFields", [])])
    context = FilterContext(
        target_entity=entity,
        stage=stage,
        intent=intent,
        context_status=status,
        missing_fields=missing,
        missing_signals=missing,
        answered_question_ids=frozenset(state.get("answeredQuestionIds", [])),
        signals=state.get("signals") if type(state.get("signals")) is dict else {},
    )
    if status is ContextResolutionStatus.NEEDS_STAGE:
        stage_question_ids = (
            ["Q_BABY_AGE_MONTHS"]
            if entity is TargetEntity.BABY
            else ["Q_PREGNANCY_TEST", "Q_GESTATIONAL_WEEK", "Q_POSTPARTUM_DAY"]
            if entity is TargetEntity.MOTHER
            else []
        )
        eligible_ids = [
            question_id
            for question_id in stage_question_ids
            if question_id not in state.get("answeredQuestionIds", [])
        ]
    else:
        eligible = eligible_questions(context, state.get("candidateQuestionIds", []))
        eligible_ids = [question.question_id for question in eligible]
    return {
        "contextResolutionStatus": status,
        "contextConflicts": list(dict.fromkeys(conflicts)),
        "candidateQuestionIds": eligible_ids,
    }


def _enum(value: object, enum_type: type[_EnumT], default: _EnumT) -> _EnumT:
    if isinstance(value, enum_type):
        return value
    if type(value) is str:
        try:
            return enum_type(value)
        except ValueError:
            pass
    return default
