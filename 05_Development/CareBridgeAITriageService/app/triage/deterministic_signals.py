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
    MATERNAL_HEAVY_BLEEDING_PHRASES,
    MATERNAL_REDUCED_FETAL_MOVEMENT_PHRASES,
    MATERNAL_SEIZURE_PHRASES,
    MATERNAL_SELF_HARM_PHRASES,
    MATERNAL_SEVERE_HEADACHE_PHRASES,
    MATERNAL_VISUAL_DISTURBANCE_PHRASES,
    normalized_text,
    text_contains_any,
    text_contains_any_current,
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

#: Stage-scoped phrase groups, ported under D-032. These mean different things for a pregnancy
#: and for a postpartum mother, so unlike the groups above they are emitted only when the stage
#: is already known from trusted context — never inferred from the sentence.
#:
#: They exist because V2 was blind to them while V1 already scored RED from the same lists
#: (``app/risk_rules.py``). Measured on the 160-case vague corpus, that gap put V2 RED recall at
#: 52.9%: "máu ra ướt đẫm hai miếng băng" and "đau đầu dữ dội kèm nhìn mờ" produced no signal at
#: all with Gemini unavailable.
_STAGE_PHRASE_SIGNALS: dict[str, tuple[tuple[tuple[str, ...], str], ...]] = {
    "PREGNANCY": (
        (MATERNAL_HEAVY_BLEEDING_PHRASES, "HEAVY_VAGINAL_BLEEDING"),
        (MATERNAL_SEVERE_HEADACHE_PHRASES, "SEVERE_HEADACHE"),
        (MATERNAL_VISUAL_DISTURBANCE_PHRASES, "VISUAL_DISTURBANCE"),
        # D-034. Pregnancy only, and deliberately absent from POSTPARTUM_MOTHER below: after
        # delivery there is no fetus to move, so the same words there describe something else
        # entirely and `GB_REDUCED_FETAL_MOVEMENT` is itself scoped to `stages: ["PREGNANCY"]`.
        # Unlike the two groups above this one reaches no RED rule — its only consumer is a
        # conservative release blocker, so what it buys is that a pregnancy reporting no fetal
        # movement can no longer be released as GREEN without being asked.
        (MATERNAL_REDUCED_FETAL_MOVEMENT_PHRASES, "REDUCED_FETAL_MOVEMENT"),
    ),
    "POSTPARTUM_MOTHER": (
        (MATERNAL_HEAVY_BLEEDING_PHRASES, "HEAVY_POSTPARTUM_BLEEDING"),
        (MATERNAL_SEVERE_HEADACHE_PHRASES, "SEVERE_HEADACHE"),
        (MATERNAL_VISUAL_DISTURBANCE_PHRASES, "VISUAL_DISTURBANCE"),
    ),
}

#: Only a stage the user or their profile actually stated may open the stage-scoped groups. A
#: stage the extractor guessed is exactly the case D-032's scope limit excludes.
_TRUSTED_STAGE_SOURCES = frozenset({
    "EXPLICIT_SELECTED_PROFILE", "CONFIRMED_CONVERSATION_TARGET",
    "EXPLICIT_IN_LATEST_MESSAGE", "EXPLICIT_CLARIFICATION_ANSWER", "STAGE_SPECIFIC_CONTEXT",
})

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


def detect_danger_signals(
    message: object, *, stage: object = None, stage_source: object = None
) -> dict[str, dict[str, object]]:
    """Danger signals evidenced by ``message``. Absence of a phrase asserts nothing.

    The entity-agnostic groups always run. The stage-scoped groups run only when ``stage`` is one
    this module knows and ``stage_source`` says the value was stated rather than inferred — see
    D-032. Callers that pass neither get exactly the previous behaviour.
    """

    if type(message) is not str or not message.strip():
        return {}
    text = normalized_text(message)
    detected = {
        code: dict(_OBSERVATION)
        for phrases, code in _PHRASE_SIGNALS
        if text_contains_any(text, *phrases)
    }
    for phrases, code in _stage_groups(stage, stage_source):
        # Current-only. Measured 2026-08-11: "Trước đây em hay nhìn mờ lúc mang thai, hiện giờ
        # em chỉ đau đầu thôi" produced VISUAL_DISTURBANCE, which completed the pre-eclampsia
        # conjunction from a sentence about a previous pregnancy — and, because a pending rule
        # supplies the planner's candidate list, pushed the global danger screen out of the turn.
        # The entity-agnostic groups above keep their unconditional reading on purpose.
        if code not in detected and text_contains_any_current(text, *phrases):
            detected[code] = dict(_OBSERVATION)
    return detected


def _stage_groups(
    stage: object, stage_source: object
) -> tuple[tuple[tuple[str, ...], str], ...]:
    """Stage-scoped groups this turn may use, or none when the stage is not trustworthy."""

    stage_value = getattr(stage, "value", stage)
    source_value = getattr(stage_source, "value", stage_source)
    if type(stage_value) is not str or stage_value not in _STAGE_PHRASE_SIGNALS:
        return ()
    if type(source_value) is not str or source_value not in _TRUSTED_STAGE_SOURCES:
        return ()
    return _STAGE_PHRASE_SIGNALS[stage_value]


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
