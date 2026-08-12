"""Stage resolution and entity–stage validation.

Two jobs, deliberately in one place because they are the same question asked twice:

1. Which lifecycle stage applies?
2. Is that stage even *possible* for the entity we resolved?

A stage that does not belong to the entity is not a rounding error. ``targetEntity=BABY`` with
``stage=PREGNANCY`` means the engine is about to reason about a pregnancy that belongs to
someone who is not the subject of the session. That is CONFLICTED, never silently corrected.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Sequence

from app.context.enums import (
    CareStage,
    ContextResolutionStatus,
    IntentType,
    ResolutionSource,
    TargetEntity,
    map_legacy_stage,
    stages_for_entity,
)
from app.context.target_entity_resolver import _fold


_NUMBER_WORD = r"(?:khong|mot|hai|ba|bon|tu|nam|sau|bay|tam|chin|muoi|linh|le|lam)"
_NUMBER = rf"(?:\d{{1,2}}|{_NUMBER_WORD}(?:\s+{_NUMBER_WORD}){{0,3}})"
_GESTATIONAL_WEEK = re.compile(
    rf"\b(?:bau|mang thai)(?:\s+(?:duoc|khoang|gan|hon))?\s+(?P<number>{_NUMBER})\s+tuan\b"
)
_BABY_AGE_MONTHS = re.compile(
    rf"(?:\b(?:be|con)(?:\s+(?:nha\s+em|cua\s+(?:toi|em)))?"
    rf"(?:\s+(?:duoc|moi|da|hien))?\s+(?P<subject_number>{_NUMBER})"
    rf"\s+thang(?:\s+tuoi)?\b|"
    rf"\b(?P<age_number>{_NUMBER})\s+thang\s+tuoi\b)"
)
_POSTPARTUM = re.compile(r"\b(?:sau sinh|hau san|moi sinh|sinh em be duoc)\b")
_NEGATED_STAGE_PREFIX = re.compile(
    r"(?:^|\s)(?:khong|chua)(?:\s+phai)?(?:\s+o)?(?:\s+giai\s+doan)?\s*$"
)

_ONES = {
    "khong": 0,
    "mot": 1,
    "hai": 2,
    "ba": 3,
    "bon": 4,
    "tu": 4,
    "nam": 5,
    "lam": 5,
    "sau": 6,
    "bay": 7,
    "tam": 8,
    "chin": 9,
}
_STAGE_CLARIFICATION_OPTIONS = {
    "STAGE_PRECONCEPTION": CareStage.PRECONCEPTION,
    "STAGE_POSSIBLE_PREGNANCY": CareStage.POSSIBLE_PREGNANCY,
    "STAGE_PREGNANCY": CareStage.PREGNANCY,
    "STAGE_POSTPARTUM_MOTHER": CareStage.POSTPARTUM_MOTHER,
}


@dataclass(frozen=True)
class StageResolution:
    stage: CareStage
    source: ResolutionSource
    conflicts: tuple[str, ...] = ()

    @property
    def is_resolved(self) -> bool:
        return self.stage.is_resolved


def resolve_stage(
    *,
    entity: TargetEntity,
    explicit_stage: CareStage | None = None,
    legacy_stage_name: str | None = None,
    journey_stage: CareStage | None = None,
    baby_age_months: int | None = None,
    gestational_week: int | None = None,
    postpartum_day: int | None = None,
    latest_user_message: str | None = None,
    submitted_option_codes: Sequence[str] | None = None,
) -> StageResolution:
    """Resolve the care stage for an already-resolved entity."""

    if not entity.is_resolved:
        # Without a subject, a stage is meaningless — PREGNANCY for whom?
        return StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE)

    valid = stages_for_entity(entity)

    clarified = {
        stage
        for code in submitted_option_codes or ()
        if (stage := _STAGE_CLARIFICATION_OPTIONS.get(code)) is not None
    }
    if len(clarified) > 1:
        return StageResolution(
            CareStage.CONFLICTED,
            ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
            ("STAGE_CLARIFICATION_ANSWERS_CONFLICT",),
        )
    if clarified:
        clarified_stage = next(iter(clarified))
        if clarified_stage not in valid:
            return StageResolution(
                CareStage.CONFLICTED,
                ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER,
                (f"STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{clarified_stage.value}",),
            )
        return StageResolution(
            clarified_stage, ResolutionSource.EXPLICIT_CLARIFICATION_ANSWER
        )

    if explicit_stage is not None and explicit_stage.is_resolved:
        if explicit_stage not in valid:
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
                (f"STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{explicit_stage.value}",))
        return StageResolution(explicit_stage, ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE)

    latest = _stage_from_latest_message(entity, latest_user_message)
    if latest is not None:
        return latest

    if legacy_stage_name:
        mapped = map_legacy_stage(legacy_stage_name, entity)
        if mapped is None:
            # Legacy POSTPARTUM with a BABY target lands here on purpose: the name is
            # ambiguous and guessing would pick the wrong subject's stage.
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.EXPLICIT_SELECTED_PROFILE,
                (f"LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY:{entity.value}/{legacy_stage_name}",))
        return StageResolution(mapped, ResolutionSource.EXPLICIT_SELECTED_PROFILE)

    if journey_stage is not None and journey_stage.is_resolved:
        if journey_stage not in valid:
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.CONFIRMED_CONVERSATION_TARGET,
                (f"JOURNEY_STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{journey_stage.value}",))
        return StageResolution(journey_stage, ResolutionSource.CONFIRMED_CONVERSATION_TARGET)

    # Derive from measurements only as a last resort, and only within the entity's own range.
    if entity is TargetEntity.BABY and baby_age_months is not None:
        if baby_age_months < 0 or baby_age_months >= 24:
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                (f"BABY_AGE_OUT_OF_SUPPORTED_RANGE:{baby_age_months}",))
        derived = CareStage.INFANT_0_12M if baby_age_months < 12 else CareStage.TODDLER_12_24M
        return StageResolution(derived, ResolutionSource.STAGE_SPECIFIC_CONTEXT)

    if entity is TargetEntity.MOTHER:
        if gestational_week is not None and postpartum_day is not None:
            # Both cannot be true at once; do not pick the more convenient one.
            return StageResolution(
                CareStage.CONFLICTED, ResolutionSource.STAGE_SPECIFIC_CONTEXT,
                ("GESTATIONAL_WEEK_AND_POSTPARTUM_DAY_BOTH_PRESENT",))
        if gestational_week is not None:
            return StageResolution(CareStage.PREGNANCY, ResolutionSource.STAGE_SPECIFIC_CONTEXT)
        if postpartum_day is not None:
            return StageResolution(CareStage.POSTPARTUM_MOTHER,
                                   ResolutionSource.STAGE_SPECIFIC_CONTEXT)

    return StageResolution(CareStage.UNKNOWN, ResolutionSource.NONE)


def _stage_from_latest_message(
    entity: TargetEntity, latest_user_message: str | None
) -> StageResolution | None:
    folded = _fold(latest_user_message)
    if not folded:
        return None

    stages: set[CareStage] = set()
    conflicts: list[str] = []

    if entity is TargetEntity.MOTHER:
        if any(
            not _is_negated(folded, match.start())
            and _parse_vietnamese_number(match.group("number")) is not None
            for match in _GESTATIONAL_WEEK.finditer(folded)
        ):
            stages.add(CareStage.PREGNANCY)
        if any(
            not _is_negated(folded, match.start())
            for match in _POSTPARTUM.finditer(folded)
        ):
            stages.add(CareStage.POSTPARTUM_MOTHER)

    elif entity is TargetEntity.BABY:
        for match in _BABY_AGE_MONTHS.finditer(folded):
            months = _parse_vietnamese_number(
                match.group("subject_number") or match.group("age_number")
            )
            if months is None:
                continue
            if months >= 24:
                conflicts.append(f"BABY_AGE_OUT_OF_SUPPORTED_RANGE:{months}")
            else:
                stages.add(
                    CareStage.INFANT_0_12M if months < 12 else CareStage.TODDLER_12_24M
                )

    if conflicts or len(stages) > 1:
        return StageResolution(
            CareStage.CONFLICTED,
            ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
            tuple(["LATEST_MESSAGE_STAGE_CONFLICT", *conflicts]),
        )
    if stages:
        return StageResolution(
            next(iter(stages)), ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE
        )
    return None


def _is_negated(message: str, match_start: int) -> bool:
    return _NEGATED_STAGE_PREFIX.search(message[:match_start]) is not None


def _parse_vietnamese_number(value: str) -> int | None:
    if value.isdigit():
        number = int(value)
        return number if 0 <= number <= 99 else None

    tokens = value.split()
    if not tokens:
        return None
    if len(tokens) == 1:
        if tokens[0] == "muoi":
            return 10
        return _ONES.get(tokens[0])

    if tokens[0] == "muoi":
        tail = next((token for token in tokens[1:] if token not in {"linh", "le"}), None)
        return 10 + _ONES[tail] if tail in _ONES else None

    if len(tokens) >= 2 and tokens[1] == "muoi" and tokens[0] in _ONES:
        number = _ONES[tokens[0]] * 10
        tail = next((token for token in tokens[2:] if token not in {"linh", "le"}), None)
        if tail is None and len(tokens) > 2:
            return None
        if tail is not None:
            if tail not in _ONES:
                return None
            number += _ONES[tail]
        return number if number <= 99 else None
    return None


def validate_entity_stage(entity: TargetEntity, stage: CareStage) -> tuple[str, ...]:
    """Return conflict codes for an entity–stage pair, empty when the pair is coherent."""

    if not entity.is_resolved or not stage.is_resolved:
        return ()
    if stage in stages_for_entity(entity):
        return ()
    return (f"STAGE_NOT_VALID_FOR_ENTITY:{entity.value}/{stage.value}",)


def resolve_context_status(
    *,
    entity: TargetEntity,
    stage: CareStage,
    intent: IntentType,
    extra_conflicts: tuple[str, ...] = (),
) -> tuple[ContextResolutionStatus, tuple[str, ...]]:
    """Combine the three resolutions into one status, plus the conflicts behind it.

    Order matters: a conflict is reported before a mere gap, because a contradiction cannot be
    fixed by asking one more question — it needs the user to choose.
    """

    conflicts = tuple(extra_conflicts) + validate_entity_stage(entity, stage)

    if entity is TargetEntity.CONFLICTED:
        conflicts += ("TARGET_ENTITY_CONFLICTED",)
    if stage is CareStage.CONFLICTED:
        conflicts += ("CARE_STAGE_CONFLICTED",)
    if intent is IntentType.CONFLICTED:
        conflicts += ("INTENT_CONFLICTED",)

    if conflicts:
        return ContextResolutionStatus.CONFLICTED, conflicts
    if not entity.is_resolved:
        return ContextResolutionStatus.NEEDS_TARGET_ENTITY, conflicts
    if not intent.is_resolved:
        return ContextResolutionStatus.NEEDS_INTENT, conflicts
    if not intent.may_produce_triage_outcome:
        # A general or source question needs no stage; it is fully resolved as it stands.
        return ContextResolutionStatus.RESOLVED, conflicts
    if not stage.is_resolved:
        return ContextResolutionStatus.NEEDS_STAGE, conflicts
    return ContextResolutionStatus.RESOLVED, conflicts
