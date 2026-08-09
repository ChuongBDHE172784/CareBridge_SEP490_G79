"""Global danger signals read deterministically from the user's own words.

V2 turns free text into signals through Gemini extraction, so with Gemini unavailable a turn
carries no signals at all. Measured on 2026-08-08: four danger sentences still degraded safely
to the explicit danger screen, but "Tôi không muốn sống nữa" produced only a generic
`Q_CLARIFY_INTENT` — a person disclosing suicidal ideation was asked to categorise their
question, and precisely when the system was already degraded.

This module gives those phrases a second, model-independent route into the signal map, using
the same matcher V1 scores its maternal red flags with (`app.danger_phrases`, one
implementation, so the two cannot drift). It is a floor, not a replacement: Gemini still adds
everything else, and `global_safety_gate` remains the only thing that decides an outcome.

Scope is deliberately the *global* danger signals — the ones that mean the same thing whatever
the stage. Stage-scoped findings such as heavy bleeding are left to the rule path, which
already asks for them; inferring them here would mean guessing the stage from a sentence.
"""

from __future__ import annotations

from typing import Mapping

from app.danger_phrases import (
    MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES,
    MATERNAL_BREATHING_DISTRESS_PHRASES,
    MATERNAL_CYANOSIS_PHRASES,
    MATERNAL_SEIZURE_PHRASES,
    MATERNAL_SELF_HARM_PHRASES,
    normalized_text,
    text_contains_any,
)

#: Phrase group -> the global signal it evidences. Self-harm reports IDEATION, never
#: INTENT_OR_PLAN: separating a thought from a plan is a clinical judgement, and a phrase list
#: cannot make it. IDEATION is the lower-bound reading and already carries the safety action.
_PHRASE_SIGNALS: tuple[tuple[tuple[str, ...], str], ...] = (
    (MATERNAL_BREATHING_DISTRESS_PHRASES, "SEVERE_BREATHING_DIFFICULTY"),
    (MATERNAL_CYANOSIS_PHRASES, "CYANOSIS"),
    (MATERNAL_SEIZURE_PHRASES, "SEIZURE"),
    (MATERNAL_ALTERED_CONSCIOUSNESS_PHRASES, "ALTERED_CONSCIOUSNESS"),
    (MATERNAL_SELF_HARM_PHRASES, "SELF_HARM_IDEATION"),
)

#: The matcher only reports a phrase the writer used and did not negate, so what it finds is
#: something they said about now, in their own words.
_OBSERVATION = {
    "presence": "PRESENT",
    "temporalStatus": "CURRENT",
    "current": True,
    "explicitNegation": False,
    "provenance": "USER_REPORTED",
    "conflictStatus": "NONE",
}


def detect_danger_signals(message: object) -> dict[str, dict[str, object]]:
    """Global danger signals evidenced by ``message``. Absence of a phrase asserts nothing."""

    if type(message) is not str or not message.strip():
        return {}
    text = normalized_text(message)
    return {
        code: dict(_OBSERVATION)
        for phrases, code in _PHRASE_SIGNALS
        if text_contains_any(text, *phrases)
    }


def merge_as_floor(
    signals: Mapping[str, object], detected: Mapping[str, dict[str, object]]
) -> dict[str, object]:
    """Add detected signals under any existing observation, never over one.

    A caller-supplied or previously extracted observation is richer than a phrase match — it
    can carry negation, history and a source question — so it wins wherever it exists. The
    floor only fills silence, which is the failure this module exists for.
    """

    merged = dict(signals) if type(signals) is dict else {}
    for code, observation in detected.items():
        merged.setdefault(code, observation)
    return merged
