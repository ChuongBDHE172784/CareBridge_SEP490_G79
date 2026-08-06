"""P1-T1 — the Python context enums must match the canonical contract exactly.

Java asserts the same file in ``ContextContractParityTest``. Declaring the values in code on
both sides keeps them ordinary enums; these tests are what stop the two declarations drifting,
which is the failure mode that previously broke rule parity.
"""

from __future__ import annotations

import pytest

from app.context import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
    load_context_contract,
    map_legacy_stage,
    stages_for_entity,
)


@pytest.fixture(scope="module")
def contract():
    return load_context_contract()


@pytest.mark.parametrize(
    "enum_type,section",
    [
        (TargetEntity, "targetEntity"),
        (CareStage, "careStage"),
        (IntentType, "intentType"),
        (ContextResolutionStatus, "contextResolutionStatus"),
        (ResolutionSource, "resolutionSource"),
    ],
)
def test_enum_matches_canonical_contract(contract, enum_type, section):
    assert [member.value for member in enum_type] == contract[section]["values"]


def test_unresolved_values_are_flagged(contract):
    assert TargetEntity.UNKNOWN.is_resolved is False
    assert TargetEntity.CONFLICTED.is_resolved is False
    assert TargetEntity.MOTHER.is_resolved is True
    assert CareStage.UNKNOWN.is_resolved is False


def test_entity_stage_mapping_matches_contract(contract):
    by_entity = contract["careStage"]["byEntity"]
    for entity in (TargetEntity.MOTHER, TargetEntity.BABY):
        assert [s.value for s in stages_for_entity(entity)] == by_entity[entity.value]

    assert stages_for_entity(TargetEntity.UNKNOWN) == ()
    assert stages_for_entity(TargetEntity.CONFLICTED) == ()


def test_stages_do_not_cross_entities():
    assert CareStage.PREGNANCY not in stages_for_entity(TargetEntity.BABY)
    assert CareStage.POSTPARTUM_MOTHER not in stages_for_entity(TargetEntity.BABY)
    assert CareStage.INFANT_0_12M not in stages_for_entity(TargetEntity.MOTHER)


def test_legacy_postpartum_maps_only_for_mother():
    """A postpartum session may be about the newborn — never auto-map it to BABY."""

    assert map_legacy_stage("POSTPARTUM", TargetEntity.MOTHER) is CareStage.POSTPARTUM_MOTHER
    assert map_legacy_stage("POSTPARTUM", TargetEntity.BABY) is None
    assert map_legacy_stage("POSTPARTUM", TargetEntity.UNKNOWN) is None
    assert map_legacy_stage("POSTPARTUM", TargetEntity.CONFLICTED) is None


def test_legacy_paediatric_stages_map_only_for_baby():
    assert map_legacy_stage("INFANT", TargetEntity.BABY) is CareStage.INFANT_0_12M
    assert map_legacy_stage("INFANT", TargetEntity.MOTHER) is None
    assert map_legacy_stage("TODDLER", TargetEntity.BABY) is CareStage.TODDLER_12_24M
    assert map_legacy_stage("UNRECOGNISED_STAGE", TargetEntity.MOTHER) is None


def test_only_triage_intents_may_produce_an_outcome(contract):
    allowed = set(contract["intentType"]["mayProduceTriageOutcome"])
    for intent in IntentType:
        assert intent.may_produce_triage_outcome is (intent.value in allowed), intent

    # Asking what a danger sign *is* must never be answered as an assessment of the user.
    assert IntentType.GENERAL_HEALTH_INFORMATION.may_produce_triage_outcome is False
    assert IntentType.SOURCE_LOOKUP.may_produce_triage_outcome is False


def test_unresolved_context_blocks_symptom_questions(contract):
    blocking = set(contract["contextResolutionStatus"]["blocksSymptomQuestions"])
    for status in ContextResolutionStatus:
        assert status.blocks_symptom_questions is (status.value in blocking), status
    assert ContextResolutionStatus.RESOLVED.blocks_symptom_questions is False


def test_resolution_precedence_matches_contract(contract):
    precedence = contract["resolutionSource"]["precedence"]
    order = [member.value for member in ResolutionSource]
    positions = [order.index(name) for name in precedence]
    assert positions == sorted(positions), "declaration order must follow contract precedence"

    # A stored profile must not override what the user just said.
    assert order.index("EXPLICIT_IN_LATEST_MESSAGE") < order.index("EXPLICIT_SELECTED_PROFILE")
    assert contract["resolutionSource"]["profileNeverOverridesExplicitInput"] is True
