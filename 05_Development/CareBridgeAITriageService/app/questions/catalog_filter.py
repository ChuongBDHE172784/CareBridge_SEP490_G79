"""Hard eligibility filter for the fixed question catalogue.

This is the gate that makes "never ask the wrong entity" an enforced property rather than a
convention. Everything the planner may consider passes through here first.

A question is eligible only when **all** of these hold:

* the target entity is resolved (unless the question exists to resolve it)
* the question lists that entity
* the stage matches, when the question constrains stage
* the intent matches, when the question constrains intent
* it resolves at least one field or signal the engine is actually missing
* it has not already been answered
* it is not a measurement the user already said they cannot provide

When context is unresolved the filter collapses to clarification questions only. Asking "bé
mấy tháng tuổi?" of a mother, or "bạn mang thai bao nhiêu tuần?" while assessing her newborn,
is not a cosmetic error — it tells the user the system has misunderstood who is unwell.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable, Sequence

from app.context.enums import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.questions.catalog import CATALOG, Question
from app.rules.condition import Presence, signal_presence


@dataclass(frozen=True)
class FilterContext:
    target_entity: TargetEntity = TargetEntity.UNKNOWN
    stage: CareStage = CareStage.UNKNOWN
    intent: IntentType = IntentType.UNKNOWN
    context_status: ContextResolutionStatus = ContextResolutionStatus.RESOLVED
    missing_fields: frozenset[str] = frozenset()
    missing_signals: frozenset[str] = frozenset()
    answered_question_ids: frozenset[str] = frozenset()
    asked_question_ids: frozenset[str] = frozenset()
    signals: dict[str, object] | None = None
    safety_screen_status: object = "INCOMPLETE"


#: Entity-agnostic danger screens. They carry no target or stage precondition, so they are safe
#: to consider in any turn; keeping the list derived from the catalogue rather than hard-coded
#: means adding another screen to the contract is enough.
_GLOBAL_DANGER_SCREEN_IDS: tuple[str, ...] = tuple(
    question_id for question_id, question in CATALOG.items()
    if question.is_global_danger_screen
)


def _clarification_for(context: FilterContext) -> tuple[str, ...]:
    """Return context clarification plus any still-required entity-agnostic danger screen."""

    if context.target_entity is TargetEntity.CONFLICTED:
        # Both named: the user picks which to assess first. One session, one subject.
        selected = ["Q_CLARIFY_TARGET_FIRST"]
    elif context.context_status is ContextResolutionStatus.NEEDS_TARGET_ENTITY:
        selected = ["Q_CLARIFY_TARGET_ENTITY"]
    elif context.context_status is ContextResolutionStatus.NEEDS_INTENT:
        selected = ["Q_CLARIFY_INTENT"]
    elif context.context_status is ContextResolutionStatus.NEEDS_STAGE:
        selected = [
            "Q_CLARIFY_STAGE"
            if context.target_entity is TargetEntity.MOTHER
            else "Q_BABY_AGE_MONTHS"
        ]
    elif context.context_status is ContextResolutionStatus.CONFLICTED:
        selected = ["Q_CLARIFY_TARGET_FIRST"]
    else:
        return ()

    safety_status = getattr(context.safety_screen_status, "value", context.safety_screen_status)
    danger = CATALOG.get("Q_GLOBAL_DANGER")
    if (
        safety_status != "COMPLETE"
        and "Q_GLOBAL_DANGER" not in context.answered_question_ids
        and "Q_GLOBAL_DANGER" not in context.asked_question_ids
        and danger is not None
        and danger.may_run_without_resolved_target
    ):
        selected.append("Q_GLOBAL_DANGER")
    return tuple(selected)


def _is_unmeasurable(question: Question, signals: dict[str, object] | None) -> bool:
    if not question.measurement or not signals:
        return False
    for code in (question.question_id, *question.resolves_signals, *question.resolves_fields):
        if signal_presence(signals, code) is Presence.UNAWARE_OR_UNMEASURABLE:
            return True
    return False


def is_eligible(
    question: Question,
    context: FilterContext,
    candidate_question_ids: Sequence[str] | None = None,
) -> bool:
    """Whether a single question may be asked in this context."""

    if (
        question.question_id in context.answered_question_ids
        or question.question_id in context.asked_question_ids
    ):
        return False

    if question.requires_resolved_target and not context.target_entity.is_resolved:
        return False

    if question.requires_resolved_target and context.target_entity not in question.target_entities:
        # The core guard: a MOTHER-only question in a BABY session, or vice versa.
        return False

    if question.applicable_stages and context.stage not in question.applicable_stages:
        return False

    if question.requires_resolved_stage and not context.stage.is_resolved:
        return False

    if question.applicable_intents and context.intent not in question.applicable_intents:
        return False

    if _is_unmeasurable(question, context.signals):
        return False

    if question.is_target_clarification or question.is_intent_clarification \
            or question.is_stage_clarification:
        # Selected by context status, not by missing fields — and only when the context
        # actually needs clarifying. Offering "whose symptom is this?" after the subject is
        # already settled would just make the system look like it was not listening.
        return context.context_status.blocks_symptom_questions

    if candidate_question_ids is not None and question.question_id in candidate_question_ids:
        return True

    resolves_something = (
        set(question.resolves_fields) & set(context.missing_fields)
        or set(question.resolves_signals) & set(context.missing_signals)
    )
    return bool(resolves_something)


def eligible_questions(
    context: FilterContext,
    candidate_question_ids: Sequence[str] | None = None,
) -> tuple[Question, ...]:
    """All questions askable in this context, highest priority first.

    While the context is unresolved this returns clarification questions only — never a
    symptom question, whatever the caller passed in.
    """

    clarifications = _clarification_for(context)
    if clarifications:
        return tuple(
            CATALOG[question_id] for question_id in clarifications
            if question_id in CATALOG
            and CATALOG[question_id].question_id not in context.answered_question_ids
            and CATALOG[question_id].question_id not in context.asked_question_ids
        )

    # A caller-supplied candidate list comes from the rules that are currently pending, and no
    # rule declares the entity-agnostic danger screen among its questionIds. Taking that list
    # literally therefore dropped the screen from every turn where any rule was pending —
    # measured 2026-08-11 as safety-question coverage falling from 100% to 98%. The screen is
    # appended rather than special-cased downstream: `is_eligible` still decides, so once its
    # signals are answered or the screen is complete it stops being returned on its own.
    candidates: Iterable[str] = (
        [*candidate_question_ids, *_GLOBAL_DANGER_SCREEN_IDS]
        if candidate_question_ids else CATALOG.keys()
    )
    eligible = [
        CATALOG[question_id] for question_id in dict.fromkeys(candidates)
        if question_id in CATALOG and is_eligible(CATALOG[question_id], context, candidate_question_ids)
    ]
    eligible.sort(key=lambda question: (question.priority, question.question_id))
    return tuple(eligible)
