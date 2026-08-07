"""Fixed clinical question catalogue, loaded from the canonical contract.

The medical content of every question is frozen here. An LLM may later rephrase for
readability, but it can never invent a question, change what a question asks, or add an
answer option: business logic keys off ``option_code``, never off display text.

``measurement`` marks questions that need a device or a number the person may simply not
have. When such a question comes back UNAWARE_OR_UNMEASURABLE, the planner pivots to the
``pivot_to`` questions — symptoms someone can perceive without equipment — rather than
asking for the same number again.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

from app.context.enums import CareStage, IntentType, TargetEntity


@dataclass(frozen=True)
class Option:
    option_code: str
    display_text: str


@dataclass(frozen=True)
class Question:
    question_id: str
    text: str
    answer_type: str
    options: tuple[Option, ...]
    resolves_signals: tuple[str, ...] = ()
    resolves_fields: tuple[str, ...] = ()
    measurement: bool = False
    pivot_to: tuple[str, ...] = ()

    #: Which subject this question can sensibly be asked about. A question with
    #: ``(MOTHER,)`` asked of a BABY session would ask a mother about her own gestation
    #: while the engine is assessing her newborn.
    target_entities: tuple[TargetEntity, ...] = (TargetEntity.MOTHER, TargetEntity.BABY)
    #: Empty means "any stage of the eligible entities".
    applicable_stages: tuple[CareStage, ...] = ()
    #: Empty means "any intent that may produce an outcome".
    applicable_intents: tuple[IntentType, ...] = ()
    priority: int = 50
    #: Signals this question can escalate, used to rank an unresolved RED first.
    escalation_signals: tuple[str, ...] = ()
    requires_resolved_target: bool = True
    requires_resolved_stage: bool = False
    is_target_clarification: bool = False
    is_stage_clarification: bool = False
    is_intent_clarification: bool = False
    #: An entity-agnostic danger screen. These signals — seizure, altered consciousness, severe
    #: breathing difficulty, cyanosis — mean the same thing for a mother and for a baby, so the
    #: question cannot be asked of the "wrong" entity and may run before the target is resolved.
    #: That is the point: an emergency must not wait for a clarification round.
    is_global_danger_screen: bool = False

    @property
    def is_clarification(self) -> bool:
        """Resolves who/what/why rather than asking about a symptom.

        Mirrors ``QuestionCatalog.Question.isClarification()`` in Java. Clarification questions
        stay available when the clinical catalogue is refused: they are how an unresolved target
        or intent gets resolved, so blocking them would stall the conversation that fixes it.
        """

        return (self.is_target_clarification
                or self.is_stage_clarification
                or self.is_intent_clarification)


CATALOG_PATH = Path(__file__).resolve().parents[2] / "data" / "question_catalog_v1.json"


@lru_cache(maxsize=1)
def _load_catalog_document() -> dict:
    return json.loads(CATALOG_PATH.read_text(encoding="utf-8"))


def _to_question(payload: dict) -> Question:
    return Question(
        question_id=payload["questionId"],
        text=payload["text"],
        answer_type=payload["answerType"],
        options=tuple(Option(o["optionCode"], o["displayText"]) for o in payload["options"]),
        resolves_signals=tuple(payload.get("resolvesSignals", ())),
        resolves_fields=tuple(payload.get("resolvesFields", ())),
        measurement=bool(payload.get("measurement", False)),
        pivot_to=tuple(payload.get("pivotTo", ())),
        target_entities=tuple(TargetEntity(v) for v in payload.get("targetEntities", ())),
        applicable_stages=tuple(CareStage(v) for v in payload.get("applicableStages", ())),
        applicable_intents=tuple(IntentType(v) for v in payload.get("applicableIntents", ())),
        priority=int(payload.get("priority", 50)),
        escalation_signals=tuple(payload.get("escalationSignals", ())),
        requires_resolved_target=bool(payload.get("requiresResolvedTarget", True)),
        requires_resolved_stage=bool(payload.get("requiresResolvedStage", False)),
        is_target_clarification=bool(payload.get("isTargetClarification", False)),
        is_stage_clarification=bool(payload.get("isStageClarification", False)),
        is_intent_clarification=bool(payload.get("isIntentClarification", False)),
        is_global_danger_screen=bool(payload.get("isGlobalDangerScreen", False)),
    )


CATALOG: dict[str, Question] = {
    payload["questionId"]: _to_question(payload)
    for payload in _load_catalog_document()["questions"]
}

MAX_QUESTIONS_PER_TURN = _load_catalog_document()["maxQuestionsPerTurn"]
MAX_ROUNDS = _load_catalog_document()["maxRounds"]


def question(question_id: str) -> Question | None:
    return CATALOG.get(question_id)
