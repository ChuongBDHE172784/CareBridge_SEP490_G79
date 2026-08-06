"""Deterministic intent resolution: what is the user actually asking for?

Intent decides whether a triage colour may be produced at all. Answering "dấu hiệu cảnh báo
thai kỳ là gì?" with RED would report an encyclopaedia question as an assessment of the person
asking — alarming, and wrong about what was even said.

Precedence, first match wins:

1. ``FOLLOW_UP_ANSWER``   — decided structurally: an option code was submitted
2. ``SOURCE_LOOKUP``      — asking where information came from
3. ``GENERAL_HEALTH_INFORMATION`` — asking what something *is*, in general
4. ``EMERGENCY_HELP``     — an explicit call for urgent help
5. ``SYMPTOM_TRIAGE``     — a first-person report of a current symptom

One deliberate asymmetry: when a message both reports a symptom and asks a general question,
the symptom report wins. Leaving a real symptom untriaged is the worse error.
"""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Mapping, Sequence

from app.context.enums import IntentType, ResolutionSource
from app.context.target_entity_resolver import _fold, _lower

INDICATORS_PATH = Path(__file__).resolve().parents[2] / "data" / "intent_indicators_v1.json"


@lru_cache(maxsize=1)
def load_intent_indicators() -> Mapping[str, object]:
    return json.loads(INDICATORS_PATH.read_text(encoding="utf-8"))


@dataclass(frozen=True)
class IntentResolution:
    intent: IntentType
    source: ResolutionSource
    evidence: tuple[str, ...] = ()

    @property
    def may_produce_triage_outcome(self) -> bool:
        return self.intent.may_produce_triage_outcome


def _hits(folded: str, phrases: Sequence[str]) -> list[str]:
    """Whole-word phrase matching — see ``target_entity_resolver._contains_any``."""

    found = []
    for phrase in phrases:
        needle = _fold(phrase)
        if needle and re.search(rf"(?<![a-z0-9]){re.escape(needle)}(?![a-z0-9])", folded):
            found.append(phrase)
    return found


def _accent_sensitive_hits(message: str | None, words: Sequence[str]) -> list[str]:
    """Whole-word matches on the ACCENTED text.

    'dấu' (sign) and 'đau' (pain) collapse to the same string once accents are stripped, so
    folded matching would read 'dấu hiệu cảnh báo là gì?' as a report of pain.
    """

    lowered = _lower(message)
    if not lowered:
        return []
    tokens = set(re.findall(r"[^\s,.;!?]+", lowered))
    return [word for word in words if word.lower() in tokens]


def resolve_intent(
    *,
    latest_user_message: str | None = None,
    submitted_option_codes: Sequence[str] | None = None,
    submitted_question_ids: Sequence[str] | None = None,
) -> IntentResolution:
    """Classify the turn's intent, or return UNKNOWN when nothing is clear enough."""

    # Structural, not textual: if the client answered a question we asked, this is a
    # follow-up regardless of how the free text reads.
    if submitted_option_codes or submitted_question_ids:
        evidence = tuple(submitted_option_codes or ()) + tuple(submitted_question_ids or ())
        return IntentResolution(IntentType.FOLLOW_UP_ANSWER,
                                ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER, evidence)

    indicators = load_intent_indicators()
    folded = _fold(latest_user_message)
    if not folded:
        return IntentResolution(IntentType.UNKNOWN, ResolutionSource.NONE)

    symptom_hits = _hits(folded, indicators["symptomTriage"]["firstPersonReportMarkers"])
    symptom_hits += _accent_sensitive_hits(
        latest_user_message, indicators["symptomTriage"]["accentSensitiveMarkers"]["words"])

    source_hits = _hits(folded, indicators["sourceLookup"]["phrases"])
    if source_hits:
        return IntentResolution(IntentType.SOURCE_LOOKUP,
                                ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, tuple(source_hits))

    out_of_scope_hits = _hits(folded, indicators["outOfScopeRequest"]["phrases"])
    if out_of_scope_hits:
        return IntentResolution(IntentType.OUT_OF_SCOPE_REQUEST,
                                ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                                tuple(out_of_scope_hits))

    general_hits = _hits(folded, indicators["generalHealthInformation"]["phrases"])
    if general_hits and not symptom_hits:
        # Only a pure question. A message that also reports a symptom falls through to
        # triage below — an untriaged real symptom is the worse failure.
        return IntentResolution(IntentType.GENERAL_HEALTH_INFORMATION,
                                ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, tuple(general_hits))

    emergency_hits = _hits(folded, indicators["emergencyHelp"]["phrases"])
    if emergency_hits and not symptom_hits:
        return IntentResolution(IntentType.EMERGENCY_HELP,
                                ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                                tuple(emergency_hits))

    if symptom_hits:
        return IntentResolution(IntentType.SYMPTOM_TRIAGE,
                                ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE, tuple(symptom_hits))

    return IntentResolution(IntentType.UNKNOWN, ResolutionSource.NONE)
