"""P1-T3 — deterministic intent resolution.

Intent decides whether a colour may be produced at all. Answering "dấu hiệu cảnh báo thai kỳ
là gì?" with RED would report an encyclopaedia question as an assessment of the person asking.
"""

from __future__ import annotations

import pytest

from app.context import resolve_intent
from app.context.enums import IntentType, ResolutionSource


def intent_of(message: str) -> IntentType:
    return resolve_intent(latest_user_message=message).intent


@pytest.mark.parametrize(
    "message",
    ["Nguồn này từ đâu vậy?", "Tài liệu tham khảo là gì", "Có đáng tin không?"],
)
def test_source_questions_are_source_lookup(message):
    assert intent_of(message) is IntentType.SOURCE_LOOKUP


@pytest.mark.parametrize(
    "message",
    [
        "Dấu hiệu cảnh báo thai kỳ là gì?",
        "Khi nào cần đi khám thai?",
        "Tiền sản giật là sao?",
        "Nên ăn gì khi mang thai?",
    ],
)
def test_general_questions_are_not_triaged(message):
    assert intent_of(message) is IntentType.GENERAL_HEALTH_INFORMATION


def test_dau_hieu_is_not_read_as_dau_pain():
    """'dấu' (sign) and 'đau' (pain) fold to the same string; accents must decide."""

    assert intent_of("Dấu hiệu cảnh báo thai kỳ là gì?") is IntentType.GENERAL_HEALTH_INFORMATION
    assert intent_of("Tôi đau bụng dữ dội") is IntentType.SYMPTOM_TRIAGE


@pytest.mark.parametrize(
    "message",
    ["Tôi bị ra máu từ sáng", "Em đang chóng mặt", "Bé bị sốt mấy ngày nay"],
)
def test_first_person_symptom_reports_are_triage(message):
    assert intent_of(message) is IntentType.SYMPTOM_TRIAGE


def test_a_symptom_report_wins_over_a_general_question():
    """Leaving a real symptom untriaged is the worse error."""

    assert intent_of("Tôi bị ra máu, dấu hiệu cảnh báo là gì?") is IntentType.SYMPTOM_TRIAGE


def test_explicit_emergency_call_is_routed_but_does_not_decide_red():
    resolution = resolve_intent(latest_user_message="Cấp cứu, phải làm gì ngay?")
    assert resolution.intent is IntentType.EMERGENCY_HELP
    # The Global Safety Gate decides RED, not the intent classifier.
    assert resolution.may_produce_triage_outcome is False


@pytest.mark.parametrize(
    "message",
    ["Kê đơn thuốc giúp tôi", "Uống thuốc gì cho hết?", "Chẩn đoán giúp em với"],
)
def test_requests_carebridge_does_not_serve_are_out_of_scope_requests(message):
    assert intent_of(message) is IntentType.OUT_OF_SCOPE_REQUEST


def test_an_answered_question_is_structurally_a_follow_up():
    """Decided by the submitted option code, not by how the free text reads."""

    resolution = resolve_intent(
        latest_user_message="Dấu hiệu cảnh báo là gì?",
        submitted_option_codes=["BLEEDING_HEAVY"],
    )
    assert resolution.intent is IntentType.FOLLOW_UP_ANSWER
    assert resolution.source is ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER
    assert resolution.may_produce_triage_outcome is True


@pytest.mark.parametrize("message", ["", "   ", "ok", "vâng"])
def test_unclassifiable_messages_stay_unknown(message):
    assert intent_of(message) is IntentType.UNKNOWN


def test_only_triage_intents_may_produce_a_colour():
    assert resolve_intent(latest_user_message="Tôi bị ra máu").may_produce_triage_outcome is True
    for message in ["Nguồn này từ đâu?", "Dấu hiệu cảnh báo là gì?", "Kê đơn giúp tôi", ""]:
        assert resolve_intent(latest_user_message=message).may_produce_triage_outcome is False


def test_evidence_is_recorded_for_the_audit_trail():
    resolution = resolve_intent(latest_user_message="Nguồn này từ đâu?")
    assert resolution.evidence
