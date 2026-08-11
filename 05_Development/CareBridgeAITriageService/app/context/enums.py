"""Shared context enums, kept in lockstep with ``Contracts/triage/context_contract_v1.json``.

The values are declared here in code (so they are ordinary enums with IDE support) and a
parity test asserts they match the canonical contract exactly. Adding a value on one side
without the other therefore fails a test instead of drifting silently, which is the failure
mode that previously broke Java/Python rule parity.
"""

from __future__ import annotations

import json
from enum import Enum
from functools import lru_cache
from pathlib import Path
from typing import Mapping

CONTRACT_PATH = Path(__file__).resolve().parents[2] / "data" / "context_contract_v1.json"


class TargetEntity(str, Enum):
    """Who the session is about. Never ``None`` — absence is UNKNOWN."""

    MOTHER = "MOTHER"
    BABY = "BABY"
    UNKNOWN = "UNKNOWN"
    #: Contradictory evidence, e.g. "tôi và bé đều bị sốt". Never resolved silently.
    CONFLICTED = "CONFLICTED"

    @property
    def is_resolved(self) -> bool:
        return self not in (TargetEntity.UNKNOWN, TargetEntity.CONFLICTED)


class CareStage(str, Enum):
    PRECONCEPTION = "PRECONCEPTION"
    POSSIBLE_PREGNANCY = "POSSIBLE_PREGNANCY"
    PREGNANCY = "PREGNANCY"
    POSTPARTUM_MOTHER = "POSTPARTUM_MOTHER"
    INFANT_0_12M = "INFANT_0_12M"
    TODDLER_12_24M = "TODDLER_12_24M"
    UNKNOWN = "UNKNOWN"
    CONFLICTED = "CONFLICTED"

    @property
    def is_resolved(self) -> bool:
        return self not in (CareStage.UNKNOWN, CareStage.CONFLICTED)


class IntentType(str, Enum):
    SYMPTOM_TRIAGE = "SYMPTOM_TRIAGE"
    GENERAL_HEALTH_INFORMATION = "GENERAL_HEALTH_INFORMATION"
    SOURCE_LOOKUP = "SOURCE_LOOKUP"
    FOLLOW_UP_ANSWER = "FOLLOW_UP_ANSWER"
    EMERGENCY_HELP = "EMERGENCY_HELP"
    OUT_OF_SCOPE_REQUEST = "OUT_OF_SCOPE_REQUEST"
    UNKNOWN = "UNKNOWN"
    CONFLICTED = "CONFLICTED"

    @property
    def is_resolved(self) -> bool:
        return self not in (IntentType.UNKNOWN, IntentType.CONFLICTED)

    @property
    def may_produce_triage_outcome(self) -> bool:
        """Asking what a danger sign *is* must never be answered with a colour."""

        return self in (IntentType.SYMPTOM_TRIAGE, IntentType.FOLLOW_UP_ANSWER)


class ContextResolutionStatus(str, Enum):
    RESOLVED = "RESOLVED"
    NEEDS_TARGET_ENTITY = "NEEDS_TARGET_ENTITY"
    NEEDS_STAGE = "NEEDS_STAGE"
    NEEDS_INTENT = "NEEDS_INTENT"
    CONFLICTED = "CONFLICTED"
    INSUFFICIENT_CONTEXT = "INSUFFICIENT_CONTEXT"

    @property
    def blocks_symptom_questions(self) -> bool:
        """Asking a symptom question before the target is known risks asking the mother
        about the baby, or applying a maternal threshold to an infant."""

        return self in (
            ContextResolutionStatus.NEEDS_TARGET_ENTITY,
            ContextResolutionStatus.NEEDS_STAGE,
            ContextResolutionStatus.NEEDS_INTENT,
            ContextResolutionStatus.CONFLICTED,
            ContextResolutionStatus.INSUFFICIENT_CONTEXT,
        )


class ResolutionSource(str, Enum):
    """Where a resolved value came from, highest precedence first."""

    EXPLICIT_CLARIFICATION_ANSWER = "EXPLICIT_CLARIFICATION_ANSWER"
    EXPLICIT_IN_LATEST_MESSAGE = "EXPLICIT_IN_LATEST_MESSAGE"
    EXPLICIT_SELECTED_PROFILE = "EXPLICIT_SELECTED_PROFILE"
    CONFIRMED_CONVERSATION_TARGET = "CONFIRMED_CONVERSATION_TARGET"
    CONFIRMED_CONVERSATION_INTENT = "CONFIRMED_CONVERSATION_INTENT"
    STAGE_SPECIFIC_CONTEXT = "STAGE_SPECIFIC_CONTEXT"
    EXTRACTOR_INFERENCE = "EXTRACTOR_INFERENCE"
    NONE = "NONE"


@lru_cache(maxsize=1)
def load_context_contract() -> Mapping[str, object]:
    return json.loads(CONTRACT_PATH.read_text(encoding="utf-8"))


def stages_for_entity(entity: TargetEntity) -> tuple[CareStage, ...]:
    """Stages valid for an entity. An unresolved entity has none — that is the point."""

    contract = load_context_contract()
    by_entity = contract["careStage"]["byEntity"]
    return tuple(CareStage(value) for value in by_entity.get(entity.value, ()))


def map_legacy_stage(legacy: str, entity: TargetEntity) -> CareStage | None:
    """Map a legacy stage name for a given target, or ``None`` when it must not be mapped.

    Legacy ``POSTPARTUM`` is the reason this exists: it named the maternal stage, but a
    postpartum session may equally be about the newborn. It maps only for MOTHER; with BABY
    or an unresolved target the caller must resolve the target first rather than guess.
    """

    contract = load_context_contract()
    mapping = contract["careStage"]["legacyStageMapping"].get(legacy)
    if mapping is None:
        return None
    mapped = mapping.get(entity.value)
    return CareStage(mapped) if mapped else None
