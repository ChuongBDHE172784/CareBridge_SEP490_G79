"""P1-T8 — deterministic out-of-scope complaint taxonomy.

OUT_OF_SCOPE requires positive evidence. "We did not understand you" is not the same
statement as "this is not our field", and conflating them would let the system dismiss
anything it failed to parse.
"""

from __future__ import annotations

import pytest

from app.context.complaint_taxonomy import (
    classify_complaint,
    may_return_out_of_scope,
)


@pytest.mark.parametrize(
    "message,category",
    [
        ("Tôi bị đau cổ tay sau khi tập thể thao", "MUSCULOSKELETAL_NON_REPRODUCTIVE"),
        ("Tôi bị đau răng mấy hôm nay", "DENTAL"),
        ("Em bị mụn trứng cá nhiều", "DERMATOLOGY_NON_REPRODUCTIVE"),
        ("Tôi bị đau mắt đỏ", "OPHTHALMOLOGY_NON_REPRODUCTIVE"),
        ("Tôi bị viêm xoang", "ENT_NON_REPRODUCTIVE"),
    ],
)
def test_known_non_reproductive_complaints_are_classified(message, category):
    assert classify_complaint(message).category_id == category


@pytest.mark.parametrize(
    "message",
    ["Tôi thấy mệt", "Có gì đó không ổn", "Tôi lo lắng quá", ""],
)
def test_an_unrecognised_complaint_is_not_out_of_scope(message):
    """Unknown means unknown — never a dismissal."""

    classification = classify_complaint(message)
    assert classification.category_id is None
    assert classification.is_confirmed_non_reproductive is False


def test_a_swollen_painful_leg_is_not_musculoskeletal_here():
    """After birth this is a green blocker, not an orthopaedic complaint."""

    classification = classify_complaint(
        "Tôi bị đau gối", signals={"UNILATERAL_LEG_SWELLING_PAIN": "PRESENT"})
    assert classification.category_id is None
    assert "MUSCULOSKELETAL_NON_REPRODUCTIVE" in classification.disqualified


def test_blurred_vision_is_not_an_eye_complaint():
    classification = classify_complaint(
        "Tôi bị đau mắt đỏ", signals={"VISUAL_DISTURBANCE": "PRESENT"})
    assert classification.category_id is None


def test_a_fall_with_bleeding_is_not_a_sports_injury():
    classification = classify_complaint(
        "Tôi bị ngã xe hôm qua", signals={"VAGINAL_BLEEDING": "PRESENT"})
    assert classification.category_id is None
    assert "INJURY_FROM_EXERCISE_OR_ACCIDENT" in classification.disqualified


# ------------------------------------------------------------------------- policy


def test_out_of_scope_needs_a_complete_safety_screen():
    """'Đau cổ tay' is not instantly out of scope — first know they are not short of breath."""

    classification = classify_complaint("Tôi bị đau cổ tay sau khi tập thể thao")
    assert may_return_out_of_scope(
        classification=classification, safety_screen_complete=False,
        has_reproductive_evidence=False) is False
    assert may_return_out_of_scope(
        classification=classification, safety_screen_complete=True,
        has_reproductive_evidence=False) is True


def test_reproductive_evidence_blocks_out_of_scope():
    classification = classify_complaint("Tôi bị đau cổ tay sau khi tập thể thao")
    assert may_return_out_of_scope(
        classification=classification, safety_screen_complete=True,
        has_reproductive_evidence=True) is False


def test_an_unclassified_complaint_can_never_be_out_of_scope():
    classification = classify_complaint("Tôi thấy mệt")
    assert may_return_out_of_scope(
        classification=classification, safety_screen_complete=True,
        has_reproductive_evidence=False) is False
