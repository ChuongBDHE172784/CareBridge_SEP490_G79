"""V2 graph adapter for deterministic intent resolution."""

from __future__ import annotations

from functools import lru_cache
from typing import Mapping

from app.context.intent_resolver import IntentResolution, resolve_intent
from app.context.enums import IntentType, ResolutionSource
from app.questions.catalog import CATALOG
from app.triage.deterministic_measurements import extract_reported_measurements


@lru_cache(maxsize=1)
def _fixed_option_codes() -> frozenset[str]:
    return frozenset(option.option_code for question in CATALOG.values() for option in question.options)


def intent_resolver(state: Mapping[str, object]) -> dict[str, object]:
    """Classify the current turn without producing any clinical result."""

    if state.get("triageOutcome") == "RED" and state.get("stopConversation") is True:
        return {}

    latest = state.get("latestUserMessage")
    message = latest if type(latest) is str else None
    submitted = list(state.get("submittedOptionCodes", []))
    if message in _fixed_option_codes() and message not in submitted:
        submitted.append(message)
    confirmed = state.get("confirmedConversationIntent")
    confirmed_intent = confirmed if isinstance(confirmed, IntentType) else None
    if confirmed_intent is None or not confirmed_intent.is_resolved:
        current = state.get("intent")
        if (
            isinstance(current, IntentType)
            and current.is_resolved
            and current is not IntentType.FOLLOW_UP_ANSWER
        ):
            confirmed_intent = current
    resolution = resolve_intent(
        latest_user_message=message,
        submitted_option_codes=submitted or None,
        confirmed_conversation_intent=confirmed_intent,
    )
    if (
        resolution.intent is IntentType.UNKNOWN
        or resolution.source is ResolutionSource.CONFIRMED_CONVERSATION_INTENT
    ) and _has_current_reported_temperature(state):
        # A valid Celsius reading with a local clinical anchor is itself an explicit current
        # health report. This lets an utterance such as "Bé hai tháng đo được 38,2 độ" reach the
        # existing paediatric rule without adding a symptom phrase or bypassing intent routing.
        resolution = IntentResolution(
            IntentType.SYMPTOM_TRIAGE,
            ResolutionSource.EXPLICIT_IN_LATEST_MESSAGE,
            ("temperatureC:USER_REPORTED_TEXT",),
        )
    updates = {"intent": resolution.intent, "intentSource": resolution.source}
    if (
        resolution.intent.is_resolved
        and resolution.intent is not IntentType.FOLLOW_UP_ANSWER
        and resolution.intent is not confirmed
    ):
        updates["confirmedConversationIntent"] = resolution.intent
    return updates


def _has_current_reported_temperature(state: Mapping[str, object]) -> bool:
    latest = state.get("latestUserMessage")
    reported = extract_reported_measurements(
        latest,
        target_entity=state.get("targetEntity"),
    )
    latest_temperature = reported.measurements.get("temperatureC")
    if type(latest_temperature) is not dict:
        return False
    measurements = state.get("measurements")
    if type(measurements) is not dict:
        return False
    temperature = measurements.get("temperatureC")
    if type(temperature) is not dict:
        return False
    value = temperature.get("value")
    return (
        type(value) in {int, float}
        and 30.0 <= value <= 45.0
        and temperature.get("unit") == "C"
        and temperature.get("temporalStatus") == "CURRENT"
        and temperature.get("provenance") == "USER_REPORTED_TEXT"
        and temperature.get("value") == latest_temperature.get("value")
    )
