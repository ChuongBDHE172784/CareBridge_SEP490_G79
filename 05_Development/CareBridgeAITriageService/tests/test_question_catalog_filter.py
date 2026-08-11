"""P1-T5/T6 — question catalogue metadata and the hard eligibility filter.

Invariant 12: the planner must never ask a question of the wrong entity. Asking "bé mấy tháng
tuổi?" of a mother tells the user the system has misunderstood who is unwell.
"""

from __future__ import annotations

import pytest

from app.context.enums import CareStage, ContextResolutionStatus, IntentType, TargetEntity
from app.questions.catalog import CATALOG
from app.questions.catalog_filter import FilterContext, eligible_questions, is_eligible


def resolved(**overrides) -> FilterContext:
    base = dict(
        target_entity=TargetEntity.MOTHER,
        stage=CareStage.PREGNANCY,
        intent=IntentType.SYMPTOM_TRIAGE,
        context_status=ContextResolutionStatus.RESOLVED,
        missing_fields=frozenset({"bleeding_amount", "pain_severity", "gestational_week"}),
        missing_signals=frozenset({"VAGINAL_BLEEDING", "VISUAL_DISTURBANCE"}),
    )
    base.update(overrides)
    return FilterContext(**base)


def ids(questions) -> list[str]:
    return [question.question_id for question in questions]


# --------------------------------------------------------------- entity enforcement


def test_a_baby_question_is_never_offered_to_a_mother():
    assert is_eligible(CATALOG["Q_BABY_AGE_MONTHS"], resolved()) is False


def test_a_mother_question_is_never_offered_for_a_baby():
    context = resolved(
        target_entity=TargetEntity.BABY,
        stage=CareStage.INFANT_0_12M,
        missing_fields=frozenset({"gestational_week", "bleeding_amount"}),
    )
    for question_id in ("Q_GESTATIONAL_WEEK", "Q_BLEEDING_AMOUNT", "Q_POSTPARTUM_DAY"):
        assert is_eligible(CATALOG[question_id], context) is False, question_id


def test_no_maternal_question_survives_a_baby_context():
    context = resolved(
        target_entity=TargetEntity.BABY,
        stage=CareStage.INFANT_0_12M,
        missing_fields=frozenset({"gestational_week", "bleeding_amount", "pain_severity"}),
        missing_signals=frozenset({"VAGINAL_BLEEDING"}),
    )
    for question in eligible_questions(context):
        assert TargetEntity.BABY in question.target_entities, question.question_id


# ------------------------------------------------------- unresolved context lockdown


def test_an_unknown_target_yields_clarification_and_global_danger():
    context = resolved(
        target_entity=TargetEntity.UNKNOWN,
        stage=CareStage.UNKNOWN,
        context_status=ContextResolutionStatus.NEEDS_TARGET_ENTITY,
    )
    assert ids(eligible_questions(context)) == ["Q_CLARIFY_TARGET_ENTITY", "Q_GLOBAL_DANGER"]


def test_a_conflicted_target_asks_which_to_assess_first_and_global_danger():
    context = resolved(
        target_entity=TargetEntity.CONFLICTED,
        context_status=ContextResolutionStatus.CONFLICTED,
    )
    assert ids(eligible_questions(context)) == ["Q_CLARIFY_TARGET_FIRST", "Q_GLOBAL_DANGER"]


def test_an_unknown_intent_asks_about_intent_and_global_danger():
    context = resolved(
        intent=IntentType.UNKNOWN,
        context_status=ContextResolutionStatus.NEEDS_INTENT,
    )
    assert ids(eligible_questions(context)) == ["Q_CLARIFY_INTENT", "Q_GLOBAL_DANGER"]


def test_a_caller_supplied_candidate_list_cannot_bypass_the_lockdown():
    """Even if the planner asks for symptom questions, unresolved context wins."""

    context = resolved(
        target_entity=TargetEntity.UNKNOWN,
        context_status=ContextResolutionStatus.NEEDS_TARGET_ENTITY,
    )
    result = eligible_questions(context, ["Q_BLEEDING_AMOUNT", "Q_BABY_AGE_MONTHS"])
    assert ids(result) == ["Q_CLARIFY_TARGET_ENTITY", "Q_GLOBAL_DANGER"]


# ------------------------------------------------------------------- stage matching


def test_a_stage_scoped_question_is_skipped_outside_its_stage():
    context = resolved(stage=CareStage.PREGNANCY, missing_fields=frozenset({"postpartum_day"}))
    assert is_eligible(CATALOG["Q_POSTPARTUM_DAY"], context) is False


def test_a_stage_scoped_question_is_offered_inside_its_stage():
    context = resolved(
        stage=CareStage.POSTPARTUM_MOTHER, missing_fields=frozenset({"postpartum_day"}))
    assert is_eligible(CATALOG["Q_POSTPARTUM_DAY"], context) is True


# --------------------------------------------------------------------- usefulness


def test_a_question_that_resolves_nothing_missing_is_not_asked():
    context = resolved(missing_fields=frozenset(), missing_signals=frozenset())
    assert eligible_questions(context) == ()


def test_an_already_answered_question_is_not_repeated():
    context = resolved(answered_question_ids=frozenset({"Q_BLEEDING_AMOUNT"}))
    assert "Q_BLEEDING_AMOUNT" not in ids(eligible_questions(context))


def test_an_unmeasurable_question_is_not_re_asked():
    context = resolved(
        missing_fields=frozenset({"blood_pressure"}),
        signals={"blood_pressure": "UNAWARE_OR_UNMEASURABLE"},
    )
    assert is_eligible(CATALOG["Q_BP_IF_KNOWN"], context) is False


def test_a_measurable_question_is_offered_when_answerable():
    context = resolved(missing_fields=frozenset({"blood_pressure"}), signals={})
    assert is_eligible(CATALOG["Q_BP_IF_KNOWN"], context) is True


# ------------------------------------------------------------------------ ordering


def test_escalating_questions_are_offered_before_routine_ones():
    context = resolved(
        missing_fields=frozenset({"bleeding_amount", "pain_severity"}),
        missing_signals=frozenset({"VISUAL_DISTURBANCE"}),
    )
    ordered = ids(eligible_questions(context))
    assert ordered.index("Q_VISUAL_CHANGE") < ordered.index("Q_PAIN_SEVERITY")


# ------------------------------------------------------------------- catalogue shape


def test_every_question_declares_at_least_one_entity():
    for question in CATALOG.values():
        assert question.target_entities, question.question_id


def test_only_clarification_or_entity_agnostic_danger_may_run_without_a_resolved_target():
    """The invariant protects against asking a wrong-entity question. A clarification question has
    no entity yet, and an entity-agnostic danger screen means the same thing for either entity —
    neither can be "wrong". Anything else must wait until we know who we are talking about."""

    for question in CATALOG.values():
        if not question.requires_resolved_target:
            assert (question.is_target_clarification
                    or question.is_intent_clarification
                    or question.is_stage_clarification
                    or question.is_global_danger_screen), question.question_id


def test_a_global_danger_screen_must_genuinely_apply_to_every_entity():
    """The exemption above is only sound while the question really is entity-agnostic. A screen
    scoped to one entity would smuggle a wrong-entity question past the target check."""

    for question in CATALOG.values():
        if question.is_global_danger_screen:
            assert set(question.target_entities) == {TargetEntity.MOTHER, TargetEntity.BABY}, (
                question.question_id
            )
            assert not question.applicable_stages, question.question_id


def test_the_deterministic_path_can_reach_every_global_danger_signal():
    """Before Q_GLOBAL_DANGER these signals were reachable only through free-text extraction, so
    an LLM outage left the engine unable to detect any emergency at all."""

    askable = {signal for question in CATALOG.values()
               for signal in (*question.resolves_signals, *question.escalation_signals)}

    for signal in ("SEIZURE", "ALTERED_CONSCIOUSNESS", "SEVERE_BREATHING_DIFFICULTY", "CYANOSIS"):
        assert signal in askable, signal


@pytest.mark.parametrize("question", list(CATALOG.values()), ids=lambda q: q.question_id)
def test_every_option_carries_a_stable_uppercase_code(question):
    for option in question.options:
        assert option.option_code.isupper()
        assert option.display_text
