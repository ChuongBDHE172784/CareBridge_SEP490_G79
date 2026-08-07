"""V2 stage and aggregate-context resolution node."""

from __future__ import annotations

from typing import Mapping, TypeVar

from app.context import CareStage, IntentType, ResolutionSource, TargetEntity
from app.context.stage_resolver import StageResolution, resolve_context_status, resolve_stage

_EnumT = TypeVar("_EnumT")
_STAGE_CONFLICT_PREFIXES = (
    "STAGE_NOT_VALID_FOR_ENTITY:",
    "JOURNEY_STAGE_NOT_VALID_FOR_ENTITY:",
    "LEGACY_STAGE_AMBIGUOUS_FOR_ENTITY:",
    "BABY_AGE_OUT_OF_SUPPORTED_RANGE:",
    "GESTATIONAL_WEEK_AND_POSTPARTUM_DAY_BOTH_PRESENT",
    "CARE_STAGE_CONFLICTED",
)


def stage_context_resolver(state: Mapping[str, object]) -> dict[str, object]:
    """Resolve stage and aggregate status without silently repairing contradictions."""

    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is True:
        return {}

    entity = _enum(state.get("targetEntity"), TargetEntity, TargetEntity.UNKNOWN)
    intent = _enum(state.get("intent"), IntentType, IntentType.UNKNOWN)
    current_stage = _enum(state.get("stage"), CareStage, CareStage.UNKNOWN)
    resolution = resolve_stage(
        entity=entity,
        journey_stage=current_stage if current_stage.is_resolved else None,
        baby_age_months=_optional_int(state.get("babyAgeMonths")),
        gestational_week=_optional_int(state.get("gestationalWeek")),
        postpartum_day=_optional_int(state.get("postpartumDay")),
    )
    if (
        resolution.stage is CareStage.UNKNOWN
        and entity is TargetEntity.MOTHER
        and state.get("possiblePregnancy") == "YES"
    ):
        resolution = StageResolution(
            CareStage.POSSIBLE_PREGNANCY, ResolutionSource.STAGE_SPECIFIC_CONTEXT
        )

    retained = [
        item
        for item in state.get("contextConflicts", [])
        if not any(item.startswith(prefix) for prefix in _STAGE_CONFLICT_PREFIXES)
    ]
    status, conflicts = resolve_context_status(
        entity=entity,
        stage=resolution.stage,
        intent=intent,
        extra_conflicts=tuple([*retained, *resolution.conflicts]),
    )
    return {
        "stage": resolution.stage,
        "contextResolutionStatus": status,
        "contextConflicts": list(dict.fromkeys(conflicts)),
    }


def _enum(value: object, enum_type: type[_EnumT], default: _EnumT) -> _EnumT:
    if isinstance(value, enum_type):
        return value
    if type(value) is str:
        try:
            return enum_type(value)
        except ValueError:
            pass
    return default


def _optional_int(value: object) -> int | None:
    return value if type(value) is int else None
