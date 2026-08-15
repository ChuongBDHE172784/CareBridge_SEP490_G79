"""V2 graph adapter for the canonical deterministic target-entity resolver."""

from __future__ import annotations

from typing import Mapping

from app.context import CareStage, ContextResolutionStatus, ResolutionSource, TargetEntity
from app.context.target_entity_resolver import resolve_target_entity

_CLARIFICATION_OPTIONS = frozenset(
    {"CLARIFY_TARGET_MOTHER", "CLARIFY_TARGET_BABY", "CLARIFY_TARGET_BOTH"}
)
_CONFLICT_CODE = "TARGET_ENTITY_CONFLICTED"


def target_entity_resolver(state: Mapping[str, object]) -> dict[str, object]:
    """Resolve exactly one session subject, returning only a partial state update."""

    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is True:
        return {}

    latest = state.get("latestUserMessage")
    submitted = state.get("submittedOptionCodes", [])
    clarification = next(
        (code for code in submitted if code in _CLARIFICATION_OPTIONS),
        latest if type(latest) is str and latest in _CLARIFICATION_OPTIONS else None,
    )
    current_target = _enum_or_none(state.get("targetEntity"), TargetEntity)
    current_source = _enum_or_none(state.get("targetEntitySource"), ResolutionSource)
    selected = (
        current_target
        if current_target is not None
        and current_target.is_resolved
        and current_source is ResolutionSource.EXPLICIT_SELECTED_PROFILE
        else None
    )
    confirmed = (
        current_target
        if current_target is not None and current_target.is_resolved and selected is None
        else None
    )
    stage = _enum_or_none(state.get("stage"), CareStage)

    resolution = resolve_target_entity(
        clarification_answer=clarification,
        latest_user_message=latest if type(latest) is str else None,
        selected_profile_entity=selected,
        confirmed_conversation_target=confirmed,
        stage=stage,
    )
    conflicts = [item for item in state.get("contextConflicts", []) if item != _CONFLICT_CODE]

    if resolution.entity is TargetEntity.CONFLICTED:
        conflicts.append(_CONFLICT_CODE)
        status = ContextResolutionStatus.CONFLICTED
    elif resolution.entity is TargetEntity.UNKNOWN:
        status = ContextResolutionStatus.NEEDS_TARGET_ENTITY
    else:
        status = ContextResolutionStatus.INSUFFICIENT_CONTEXT

    return {
        "targetEntity": resolution.entity,
        "targetEntitySource": resolution.source,
        "contextResolutionStatus": status,
        "contextConflicts": conflicts,
    }


def _enum_or_none(value: object, enum_type):
    if isinstance(value, enum_type):
        return value
    if type(value) is str:
        try:
            return enum_type(value)
        except ValueError:
            return None
    return None
