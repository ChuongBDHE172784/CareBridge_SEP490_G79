"""Deterministic signal normalization and conflict merging for triage."""

from __future__ import annotations

from copy import deepcopy
from typing import Mapping

from app.rules.condition import Presence

_SIGNAL_CONFLICT_PREFIX = "SIGNAL_CONFLICTED:"
_CURRENT = "CURRENT"
_HISTORICAL = "HISTORICAL"
#: Remembered background, not an observation of now. A profile note or a health-memory entry
#: saying the user once had a symptom must never present itself as a current finding: that is how
#: a stale record turns into a live danger signal nobody actually reported this session. Such an
#: observation is demoted to historical, where it can still inform questions but cannot fire a
#: current rule. It is never demoted to ABSENT â€” we do not know that it is absent.
_REMEMBERED_PROVENANCE = frozenset({"PROFILE_CONTEXT", "HEALTH_MEMORY_CONTEXT"})


def signal_normalizer(state: Mapping[str, object]) -> dict[str, object]:
    """Normalize structured observations; never infer clinical severity or thresholds."""

    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is True:
        return {}

    source = state.get("signals")
    normalized: dict[str, object] = {}
    conflict_codes: list[str] = []
    if type(source) is dict:
        for code, raw in source.items():
            if type(code) is not str:
                continue
            merged = normalize_signal_observation(raw)
            normalized[code] = merged
            if merged["presence"] == Presence.CONFLICTED.value:
                conflict_codes.append(f"{_SIGNAL_CONFLICT_PREFIX}{code}")

    prior_conflicts = [
        item
        for item in state.get("dataConflicts", [])
        if not item.startswith(_SIGNAL_CONFLICT_PREFIX)
    ]
    return {
        "signals": normalized,
        "measurements": _normalize_measurements(state.get("measurements")),
        "dataConflicts": list(dict.fromkeys([*prior_conflicts, *conflict_codes])),
    }


def normalize_signal_observation(raw: object) -> dict[str, object]:
    """Normalize one value for reuse by the pre-context Global Safety Gate."""
    observations = raw if type(raw) is list else [raw]
    current: list[Presence] = []
    historical: list[Presence] = []

    for observation in observations:
        presence, temporal = _observation(observation)
        if temporal == _HISTORICAL:
            historical.append(presence)
        elif temporal == _CURRENT:
            current.append(presence)

    merged = _merge_presence(current)
    result: dict[str, object] = {"presence": merged.value, "temporalStatus": _CURRENT}
    if historical:
        result["historicalPresence"] = _merge_presence(historical).value
    return result


def _observation(raw: object) -> tuple[Presence, str]:
    if isinstance(raw, Presence):
        return raw, _CURRENT
    if type(raw) is str:
        return _presence(raw), _CURRENT
    if type(raw) is not dict:
        return Presence.UNKNOWN, _CURRENT

    temporal = raw.get("temporalStatus", raw.get("currentVsHistorical", _CURRENT))
    if temporal not in {_CURRENT, _HISTORICAL}:
        temporal = _CURRENT
    presence = _presence(raw.get("presence"))
    if raw.get("explicitNegation") is True:
        if presence is Presence.PRESENT:
            presence = Presence.CONFLICTED
        else:
            presence = Presence.ABSENT
    if raw.get("provenance") in _REMEMBERED_PROVENANCE and presence is Presence.PRESENT:
        temporal = _HISTORICAL
    return presence, temporal


def _presence(value: object) -> Presence:
    if type(value) is not str:
        return Presence.UNKNOWN
    try:
        return Presence(value)
    except ValueError:
        return Presence.UNKNOWN


def _merge_presence(values: list[Presence]) -> Presence:
    if Presence.CONFLICTED in values:
        return Presence.CONFLICTED
    resolved = {value for value in values if value in {Presence.PRESENT, Presence.ABSENT}}
    if len(resolved) > 1:
        return Presence.CONFLICTED
    if resolved:
        return next(iter(resolved))
    if Presence.UNAWARE_OR_UNMEASURABLE in values:
        return Presence.UNAWARE_OR_UNMEASURABLE
    return Presence.UNKNOWN


def _normalize_measurements(value: object) -> dict[str, object]:
    if type(value) is not dict:
        return {}
    normalized: dict[str, object] = {}
    for code, measurement in value.items():
        if type(code) is not str or type(measurement) is bool:
            continue
        if type(measurement) in {int, float}:
            normalized[code] = {"value": measurement}
        elif type(measurement) is dict:
            normalized[code] = deepcopy(measurement)
        elif measurement == Presence.UNAWARE_OR_UNMEASURABLE.value:
            normalized[code] = {"status": Presence.UNAWARE_OR_UNMEASURABLE.value}
    return normalized
