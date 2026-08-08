"""V2's danger floor: what it must catch without Gemini, and what it must not invent.

Both halves matter equally. The floor exists because a self-harm disclosure produced only a
generic clarification when extraction was unavailable; it earns its place only for as long as
it does not also turn ordinary sentences into emergencies.
"""

from __future__ import annotations

import json
import pathlib
import uuid

import pytest

import app.triage_v2.api as api
from app.rules.registry import get_registry
from app.triage_v2.deterministic_signals import detect_danger_signals, merge_as_floor


CORPUS = json.loads(
    (pathlib.Path(__file__).parent / "data" / "vietnamese_intake_corpus.json").read_text(
        encoding="utf-8"
    )
)
#: The corpus speaks the V1 wire vocabulary; V2 takes the entity-split stage names.
_STAGE = {
    "POSTPARTUM": ("POSTPARTUM_MOTHER", {"postpartumDay": 10}, "MOTHER"),
    "PREGNANCY": ("PREGNANCY", {"gestationalWeek": 28}, "MOTHER"),
    "PRECONCEPTION": ("PRECONCEPTION", {}, "MOTHER"),
}


def _turn(message: str, stage: str):
    care_stage, extra, target = _STAGE[stage]
    payload = {
        "sessionId": str(uuid.uuid4()),
        "stateVersion": 0,
        "expectedStateVersion": 0,
        "requestId": "request_" + uuid.uuid4().hex[:16],
        "messageId": "message_" + uuid.uuid4().hex[:16],
        "latestUserMessage": message,
        "selectedTarget": target,
        "journeyContext": {"stage": care_stage, **extra},
        # Empty: this is a turn where extraction produced nothing.
        "signals": {},
        "measurements": {},
        "expectedRulesetHash": get_registry().ruleset_sha256,
    }
    return api.execute_turn(api.TriageV2TurnRequest.model_validate(payload)).state


@pytest.mark.parametrize(
    ("message", "code"),
    [
        # The case this module was written for: previously only Q_CLARIFY_INTENT.
        ("Tôi không muốn sống nữa", "SELF_HARM_IDEATION"),
        ("Tôi muốn chết", "SELF_HARM_IDEATION"),
        ("Tôi bị co giật", "SEIZURE"),
        ("Tôi khó thở dữ dội", "SEVERE_BREATHING_DIFFICULTY"),
        ("Môi tôi tím tái", "CYANOSIS"),
        ("Bé nhà em li bì khó đánh thức", "ALTERED_CONSCIOUSNESS"),
        # Accent-free typing must reach the same floor.
        ("toi bi co giat", "SEIZURE"),
    ],
)
def test_danger_phrase_becomes_a_signal(message, code):
    assert code in detect_danger_signals(message)


@pytest.mark.parametrize(
    "message",
    [case["text"] for case in CORPUS["cases"] if case["expect"] == "NOT_RED"],
)
def test_ordinary_sentences_produce_no_danger_signal(message):
    assert detect_danger_signals(message) == {}


def test_floor_never_overwrites_a_richer_observation():
    """An explicit ABSENT from a question answer outranks a phrase match, not the reverse."""

    answered = {
        "SEIZURE": {
            "presence": "ABSENT",
            "provenance": "QUESTION_ANSWER",
            "sourceQuestionId": "Q_GLOBAL_DANGER",
        }
    }
    merged = merge_as_floor(answered, detect_danger_signals("Tôi bị co giật"))

    assert merged["SEIZURE"]["presence"] == "ABSENT"
    assert merged["SEIZURE"]["provenance"] == "QUESTION_ANSWER"


def test_floor_fills_only_what_is_missing():
    merged = merge_as_floor(
        {"SEIZURE": {"presence": "ABSENT"}},
        detect_danger_signals("Tôi bị co giật và khó thở dữ dội"),
    )

    assert merged["SEIZURE"]["presence"] == "ABSENT"
    assert merged["SEVERE_BREATHING_DIFFICULTY"]["presence"] == "PRESENT"


@pytest.mark.parametrize("message", ["", "   ", None, 123])
def test_non_text_input_asserts_nothing(message):
    assert detect_danger_signals(message) == {}


def test_self_harm_disclosure_reaches_safety_support_without_gemini(monkeypatch):
    """End to end: the turn that used to answer with a clarification question."""

    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    state = _turn("Tôi không muốn sống nữa", "POSTPARTUM")

    assert state.get("triageOutcome") == "RED"
    assert state.get("requiredAction") == "IMMEDIATE_SAFETY_SUPPORT"
    assert "SAFETY_RISK_SELF_OR_INFANT_HARM" in (state.get("reasonCodes") or [])


@pytest.mark.parametrize(
    "case",
    [c for c in CORPUS["cases"] if c["expect"] == "NOT_RED"],
    ids=lambda c: c["text"][:38],
)
def test_ordinary_sentences_do_not_reach_red_in_v2(case, monkeypatch):
    monkeypatch.setattr(api, "get_gemini_client", lambda: None)
    state = _turn(case["text"], case["stage"])

    assert state.get("triageOutcome") != "RED", (
        f"{case['text']!r} escalated on {state.get('reasonCodes')}"
    )
