"""Runs the engine against sentences as people write them, not against field assignments.

The rest of the suite pins structure: which rule fires for which signal, which stage owns
which question, whether an artifact loads. That is worth having, and it is also why
"Tôi có giặt quần áo cho bé mỗi ngày" reached an emergency escalation with ~1600 tests
green — nothing was checking what the engine does with a sentence.

Both directions are asserted from one corpus on purpose. A missed haemorrhage and an
escalated laundry sentence are the same defect seen from two sides, and fixing either one
alone tends to cause the other.
"""

import json
from pathlib import Path

import pytest

from app.graph import run_triage
from app.schemas import ChildTriageRequest


CORPUS = json.loads(
    (Path(__file__).parent / "data" / "vietnamese_intake_corpus.json").read_text(
        encoding="utf-8"
    )
)
CASES = CORPUS["cases"]


def _case_id(case: dict) -> str:
    return f"{case['expect']}-{case['stage']}-{case['text'][:38]}"


def _run(case: dict):
    return run_triage(
        ChildTriageRequest(
            stage=case["stage"],
            symptomList=[case["text"]],
            parentFreeText=case["text"],
            # Deliberately unanswered: this is the first turn, which is exactly when the
            # free text is all the engine has and when the emergency route is decided.
        ),
        deterministic_only=True,
    )


def _red_rules(response) -> list[str]:
    return [rule for rule in response.matchedRules if rule.startswith("RED_")]


@pytest.mark.parametrize(
    "case", [c for c in CASES if c["expect"] == "RED"], ids=_case_id
)
def test_danger_sentences_reach_red(case):
    response = _run(case)

    assert response.riskLevel == "RED", (
        f"[{case['group']}] {case['text']!r} did not reach RED "
        f"(got {response.riskLevel}, rules={response.matchedRules})"
    )
    assert response.emergencyActionRequired is True


@pytest.mark.parametrize(
    "case", [c for c in CASES if c["expect"] == "NOT_RED"], ids=_case_id
)
def test_ordinary_sentences_do_not_reach_red(case):
    response = _run(case)

    assert not _red_rules(response), (
        f"[{case['group']}] {case['text']!r} escalated on "
        f"{_red_rules(response)} — an ordinary sentence must not open the emergency route"
    )
    assert response.emergencyActionRequired is False


def test_corpus_covers_both_directions():
    """A corpus that drifts to one side stops being able to catch the other."""

    red = sum(1 for c in CASES if c["expect"] == "RED")
    not_red = sum(1 for c in CASES if c["expect"] == "NOT_RED")

    assert red >= 15, "too few danger sentences to be evidence of anything"
    assert not_red >= 15, "without ordinary sentences this only measures over-calling"
    assert len({c["text"] for c in CASES}) == len(CASES), "duplicate sentence in corpus"
