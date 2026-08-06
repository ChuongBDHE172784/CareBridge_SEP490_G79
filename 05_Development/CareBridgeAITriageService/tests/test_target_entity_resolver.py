"""P1-T2 — deterministic target-entity resolution.

Deciding "mother" for a message about an infant is not a UX slip: it applies maternal
thresholds to a baby and asks the wrong questions. These tests pin the conservative
behaviour — UNKNOWN rather than guess, CONFLICTED rather than pick a side.
"""

from __future__ import annotations

import pytest

from app.context.enums import CareStage, ResolutionSource, TargetEntity
from app.context.target_entity_resolver import (
    resolve_target_entity,
    score_message,
)


@pytest.mark.parametrize(
    "message,expected",
    [
        ("Tôi bị tiêu chảy", TargetEntity.MOTHER),
        ("Em đang mang thai 30 tuần", TargetEntity.MOTHER),
        ("Tôi sau sinh 5 ngày ra máu nhiều", TargetEntity.MOTHER),
        ("Sản dịch có mùi hôi", TargetEntity.MOTHER),
        ("Bé bị đi ngoài phân lỏng", TargetEntity.BABY),
        ("Con tôi sốt cao", TargetEntity.BABY),
        ("Bé nhà em bỏ bú", TargetEntity.BABY),
        ("Trẻ sơ sinh bị vàng da", TargetEntity.BABY),
    ],
)
def test_unambiguous_messages_resolve(message, expected):
    assert resolve_target_entity(latest_user_message=message).entity is expected


def test_possessed_child_outranks_the_first_person_pronoun():
    """"Tôi hỏi giúp con tôi" is about the baby, despite starting with "tôi"."""

    resolution = resolve_target_entity(latest_user_message="Tôi hỏi giúp con tôi, bé bị sốt")
    assert resolution.entity is TargetEntity.BABY
    assert resolution.source is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE


def test_conjunction_con_is_not_mistaken_for_the_word_child():
    """The conjunction "còn" folds to "con" once accents are stripped.

    Splitting clauses on folded text would cut "giúp con tôi" in half and lose the baby.
    """

    assert resolve_target_entity(
        latest_user_message="Tôi đau bụng còn bé thì sốt").entity is TargetEntity.CONFLICTED
    assert resolve_target_entity(
        latest_user_message="Tôi hỏi giúp con tôi").entity is TargetEntity.BABY


@pytest.mark.parametrize(
    "message",
    [
        "Tôi và bé đều bị sốt",
        "Cả hai mẹ con đều mệt",
        "Mẹ và bé cùng bị ho",
    ],
)
def test_multi_entity_messages_are_conflicted_not_guessed(message):
    resolution = resolve_target_entity(latest_user_message=message)
    assert resolution.entity is TargetEntity.CONFLICTED
    assert resolution.conflict_evidence, "the UI must be able to say what it saw"


def test_both_entities_across_clauses_is_conflicted():
    assert resolve_target_entity(
        latest_user_message="Tôi bị sốt, bé cũng bị sốt").entity is TargetEntity.CONFLICTED


@pytest.mark.parametrize(
    "message",
    ["Bị sốt 38.5 độ từ sáng", "Đau bụng dưới", "Chóng mặt từ hôm qua", ""],
)
def test_ambiguous_messages_stay_unknown(message):
    """No subject named — guessing here is how a baby gets maternal thresholds."""

    resolution = resolve_target_entity(latest_user_message=message)
    assert resolution.entity is TargetEntity.UNKNOWN
    assert resolution.source is ResolutionSource.NONE


def test_a_third_party_is_not_scored_as_the_user():
    """A friend's symptom must not silently become the user's triage."""

    assert resolve_target_entity(
        latest_user_message="Bạn tôi bị đau bụng").entity is TargetEntity.UNKNOWN


# ------------------------------------------------------------------------ precedence


def test_clarification_answer_outranks_everything():
    resolution = resolve_target_entity(
        clarification_answer="CLARIFY_TARGET_BABY",
        latest_user_message="Tôi sau sinh 5 ngày",
        selected_profile_entity=TargetEntity.MOTHER,
    )
    assert resolution.entity is TargetEntity.BABY
    assert resolution.source is ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER


def test_clarify_both_becomes_conflicted_not_a_silent_pick():
    resolution = resolve_target_entity(clarification_answer="CLARIFY_TARGET_BOTH")
    assert resolution.entity is TargetEntity.CONFLICTED


def test_latest_message_outranks_a_stored_profile():
    """A stored profile must never override what the user just typed."""

    resolution = resolve_target_entity(
        latest_user_message="Bé nhà em bỏ bú",
        selected_profile_entity=TargetEntity.MOTHER,
    )
    assert resolution.entity is TargetEntity.BABY
    assert resolution.source is ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE


def test_profile_is_used_only_when_the_message_says_nothing():
    resolution = resolve_target_entity(
        latest_user_message="Bị sốt 38.5 độ",
        selected_profile_entity=TargetEntity.BABY,
    )
    assert resolution.entity is TargetEntity.BABY
    assert resolution.source is ResolutionSource.EXPLICIT_SELECTED_PROFILE


def test_confirmed_conversation_target_is_used_below_the_profile():
    resolution = resolve_target_entity(
        latest_user_message="Vẫn còn mệt",
        confirmed_conversation_target=TargetEntity.MOTHER,
    )
    assert resolution.source is ResolutionSource.CONFIRMED_CONVERSATION_TARGET


@pytest.mark.parametrize(
    "stage,expected",
    [
        (CareStage.PREGNANCY, TargetEntity.MOTHER),
        (CareStage.PRECONCEPTION, TargetEntity.MOTHER),
        (CareStage.INFANT_0_12M, TargetEntity.BABY),
        (CareStage.TODDLER_12_24M, TargetEntity.BABY),
    ],
)
def test_stage_context_is_the_weakest_resolved_signal(stage, expected):
    resolution = resolve_target_entity(latest_user_message="Bị sốt", stage=stage)
    assert resolution.entity is expected
    assert resolution.source is ResolutionSource.STAGE_SPECIFIC_CONTEXT


def test_postpartum_stage_alone_does_not_resolve_the_target():
    """Postpartum is exactly when a newborn question is most likely — it is not evidence."""

    resolution = resolve_target_entity(
        latest_user_message="Bị sốt 38.5 độ", stage=CareStage.POSTPARTUM_MOTHER)
    assert resolution.entity is TargetEntity.UNKNOWN


def test_unresolved_inputs_are_ignored_rather_than_trusted():
    resolution = resolve_target_entity(
        latest_user_message="Bị sốt",
        selected_profile_entity=TargetEntity.UNKNOWN,
        confirmed_conversation_target=TargetEntity.CONFLICTED,
    )
    assert resolution.entity is TargetEntity.UNKNOWN


def test_evidence_is_recorded_for_the_audit_trail():
    resolution = resolve_target_entity(latest_user_message="Con tôi sốt cao")
    assert resolution.entity is TargetEntity.BABY
    assert resolution.evidence, "the decision must be explainable"


def test_score_message_is_pure_and_order_independent():
    first = score_message("Bé nhà em bỏ bú")
    second = score_message("Bé nhà em bỏ bú")
    assert first == second
