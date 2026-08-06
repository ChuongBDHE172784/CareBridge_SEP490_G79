"""Deterministic target-entity resolution: is this session about the mother or the baby?

Getting this wrong is not a UX bug. If the engine decides "mother" for a message about an
infant, it will apply maternal thresholds to a baby and ask the wrong questions. So the
resolver is deliberately conservative: it returns UNKNOWN rather than guess, and CONFLICTED
rather than pick a side.

Precedence (highest first), matching ``ResolutionSource``:

1. an explicit clarification answer in this session
2. an explicit subject in the latest message
3. an explicitly selected profile in the request
4. a target the user already confirmed earlier in the conversation
5. strong stage-specific context
6. extractor inference (Phase 4; not wired yet)

A stored profile never overrides what the user just typed — hence 2 above 3.
"""

from __future__ import annotations

import json
import re
import unicodedata
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Mapping, Sequence

from app.context.enums import CareStage, ResolutionSource, TargetEntity

INDICATORS_PATH = Path(__file__).resolve().parents[2] / "data" / "target_entity_indicators_v1.json"


@lru_cache(maxsize=1)
def load_indicators() -> Mapping[str, object]:
    return json.loads(INDICATORS_PATH.read_text(encoding="utf-8"))


@dataclass(frozen=True)
class TargetResolution:
    entity: TargetEntity
    source: ResolutionSource
    #: Surface cues that drove the decision, for the audit trail.
    evidence: tuple[str, ...] = ()
    #: Populated when both entities are named, so the UI can say what it saw.
    conflict_evidence: tuple[str, ...] = ()

    @property
    def is_resolved(self) -> bool:
        return self.entity.is_resolved


def _fold(text: str | None) -> str:
    """Lowercase, strip Vietnamese accents, collapse whitespace."""

    if not text:
        return ""
    lowered = text.lower().replace("đ", "d")
    stripped = "".join(
        char for char in unicodedata.normalize("NFD", lowered)
        if unicodedata.category(char) != "Mn"
    )
    return re.sub(r"\s+", " ", stripped).strip()


def _lower(text: str | None) -> str:
    """Lowercase and collapse whitespace, KEEPING accents."""

    return re.sub(r"\s+", " ", (text or "").lower()).strip()


def _clauses(text: str | None) -> list[str]:
    """Split into clauses on the ACCENTED text, then fold each clause for matching.

    Splitting on folded text is wrong in Vietnamese: the conjunction "còn" folds to "con",
    which is the word for a child. "giúp con tôi" would then be cut in half and the baby
    reference lost. Accents are only safe to drop once the boundaries are already fixed.
    """

    rules = load_indicators()["matchingRules"]
    separators = [sep.lower() for sep in rules["clauseSeparators"] if sep.strip() or sep in ",.;!?"]
    pattern = "|".join(re.escape(sep) for sep in separators)
    lowered = _lower(text)
    if not pattern:
        return [_fold(lowered)] if lowered else []
    parts = [part.strip() for part in re.split(pattern, lowered)]
    return [_fold(part) for part in parts if part.strip()]


def _contains_any(haystack: str, phrases: Sequence[str]) -> list[str]:
    """Whole-word phrase matching.

    Raw substring matching is unsafe here: "tã" (nappy) folds to "ta", which then matches
    inside "tay" (hand) and "tập" (exercise), so "đau cổ tay sau khi tập" scored as a baby
    message. Short Vietnamese tokens make this failure mode easy to hit, so every phrase is
    anchored on both sides.
    """

    found = []
    for phrase in phrases:
        needle = _fold(phrase)
        if not needle:
            continue
        if re.search(rf"(?<![a-z0-9]){re.escape(needle)}(?![a-z0-9])", haystack):
            found.append(phrase)
    return found


def score_message(message: str | None) -> tuple[TargetEntity, tuple[str, ...], tuple[str, ...]]:
    """Score a raw message. Returns ``(entity, evidence, conflict_evidence)``."""

    indicators = load_indicators()
    folded = _fold(message)
    if not folded:
        return TargetEntity.UNKNOWN, (), ()
    clauses = _clauses(message)

    multi = _contains_any(folded, indicators["multiEntityPhrases"]["phrases"])
    if multi:
        # "Tôi và bé đều bị sốt" — never silently pick one. One session, one target.
        return TargetEntity.CONFLICTED, (), tuple(multi)

    third_party = _contains_any(folded, indicators["thirdPartyPhrases"]["phrases"])

    mother_hits: list[str] = []
    baby_hits: list[str] = []

    for clause in clauses:
        baby_possessive = _contains_any(clause, indicators["baby"]["possessiveOrSubject"])
        baby_strong = _contains_any(clause, indicators["baby"]["strongIndicators"])
        mother_strong = _contains_any(clause, indicators["mother"]["strongIndicators"])
        mother_subject = _contains_any(clause, indicators["mother"]["possessiveOrSubject"])

        if baby_possessive or baby_strong:
            # A possessed child in the clause outranks the first-person pronoun that
            # introduced it: "tôi hỏi giúp con tôi, bé bị sốt" is about the baby.
            baby_hits.extend(baby_possessive + baby_strong)
            continue
        if mother_strong:
            mother_hits.extend(mother_strong)
            continue
        if mother_subject and not third_party:
            mother_hits.extend(mother_subject)

    if mother_hits and baby_hits:
        return TargetEntity.CONFLICTED, (), tuple(dict.fromkeys(mother_hits + baby_hits))
    if baby_hits:
        return TargetEntity.BABY, tuple(dict.fromkeys(baby_hits)), ()
    if mother_hits:
        return TargetEntity.MOTHER, tuple(dict.fromkeys(mother_hits)), ()
    return TargetEntity.UNKNOWN, (), ()


def _entity_from_clarification(answer: str | None) -> TargetEntity | None:
    return {
        "CLARIFY_TARGET_MOTHER": TargetEntity.MOTHER,
        "CLARIFY_TARGET_BABY": TargetEntity.BABY,
        "CLARIFY_TARGET_BOTH": TargetEntity.CONFLICTED,
    }.get(answer or "")


def _entity_from_stage(stage: CareStage | None) -> TargetEntity | None:
    if stage is None or not stage.is_resolved:
        return None
    if stage in (CareStage.INFANT_0_12M, CareStage.TODDLER_12_24M):
        return TargetEntity.BABY
    if stage in (CareStage.PRECONCEPTION, CareStage.POSSIBLE_PREGNANCY, CareStage.PREGNANCY):
        return TargetEntity.MOTHER
    # POSTPARTUM_MOTHER names the mother's stage, but a postpartum session is exactly when a
    # newborn question is most likely. It is not strong enough evidence on its own.
    return None


def resolve_target_entity(
    *,
    clarification_answer: str | None = None,
    latest_user_message: str | None = None,
    selected_profile_entity: TargetEntity | None = None,
    confirmed_conversation_target: TargetEntity | None = None,
    stage: CareStage | None = None,
) -> TargetResolution:
    """Resolve the session's target entity, or report why it could not be resolved."""

    clarified = _entity_from_clarification(clarification_answer)
    if clarified is not None:
        return TargetResolution(clarified, ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
                                evidence=(clarification_answer,))

    entity, evidence, conflicts = score_message(latest_user_message)
    if entity is TargetEntity.CONFLICTED:
        return TargetResolution(entity, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                                conflict_evidence=conflicts)
    if entity.is_resolved:
        return TargetResolution(entity, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, evidence)

    if selected_profile_entity is not None and selected_profile_entity.is_resolved:
        return TargetResolution(selected_profile_entity, ResolutionSource.EXPLICIT_SELECTED_PROFILE)

    if confirmed_conversation_target is not None and confirmed_conversation_target.is_resolved:
        return TargetResolution(confirmed_conversation_target,
                                ResolutionSource.CONFIRMED_CONVERSATION_TARGET)

    from_stage = _entity_from_stage(stage)
    if from_stage is not None:
        return TargetResolution(from_stage, ResolutionSource.STAGE_SPECIFIC_CONTEXT)

    return TargetResolution(TargetEntity.UNKNOWN, ResolutionSource.NONE)
