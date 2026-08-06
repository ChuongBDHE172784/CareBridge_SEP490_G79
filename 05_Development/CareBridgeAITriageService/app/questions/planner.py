"""Deterministic question planner.

Selection rules, in order:

1. An unresolved RED signal always wins. If a RED rule is TRUE on one signal and merely
   unanswered on another, the question that resolves the unanswered one is asked first —
   that case must never be parked.
2. A measurement question already answered "no device / not sure" is never re-asked.
   The planner pivots to its ``pivot_to`` questions instead: symptoms the person can
   perceive without equipment.
3. Everything else comes from the rules' declared ``questionIds``, in order.

Never more than ``maximum_question_rounds`` rounds, and never more than three questions
in one turn.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping, Sequence

from app.questions.catalog import Question, question
from app.rules.condition import Presence, signal_presence

MAX_QUESTIONS_PER_TURN = 3


@dataclass(frozen=True)
class PlannedQuestions:
    questions: tuple[Question, ...]
    pivoted_from: tuple[str, ...] = ()

    @property
    def question_ids(self) -> tuple[str, ...]:
        return tuple(item.question_id for item in self.questions)


def plan_questions(
    *,
    candidate_question_ids: Sequence[str],
    signals: Mapping[str, Any],
    answered_question_ids: Sequence[str] = (),
    priority_question_ids: Sequence[str] = (),
) -> PlannedQuestions:
    """Choose the next questions, resolving unanswerable measurements by pivoting."""

    ordered = list(dict.fromkeys(list(priority_question_ids) + list(candidate_question_ids)))
    answered = set(answered_question_ids)

    selected: list[Question] = []
    pivoted_from: list[str] = []
    seen: set[str] = set()

    def add(question_id: str) -> None:
        if question_id in seen or question_id in answered or len(selected) >= MAX_QUESTIONS_PER_TURN:
            return
        item = question(question_id)
        if item is None:
            return
        seen.add(question_id)
        selected.append(item)

    for question_id in ordered:
        item = question(question_id)
        if item is None:
            continue
        if item.measurement and _is_unmeasurable(item, signals):
            # Asking again would only repeat a question the person already cannot answer.
            pivoted_from.append(question_id)
            seen.add(question_id)
            for pivot_id in item.pivot_to:
                add(pivot_id)
            continue
        add(question_id)

    return PlannedQuestions(tuple(selected[:MAX_QUESTIONS_PER_TURN]), tuple(pivoted_from))


def _is_unmeasurable(item: Question, signals: Mapping[str, Any]) -> bool:
    """True when this measurement question has already come back as unanswerable."""

    marker = signals.get(item.question_id)
    if marker is not None:
        presence = signal_presence(signals, item.question_id)
        if presence is Presence.UNAWARE_OR_UNMEASURABLE:
            return True
    for code in item.resolves_signals + item.resolves_fields:
        if signal_presence(signals, code) is Presence.UNAWARE_OR_UNMEASURABLE:
            return True
    return False
