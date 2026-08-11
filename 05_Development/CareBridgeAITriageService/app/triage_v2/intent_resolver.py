"""V2 graph adapter for deterministic intent resolution."""

from __future__ import annotations

from functools import lru_cache
from typing import Mapping

from app.context.intent_resolver import resolve_intent
from app.context.enums import IntentType
from app.questions.catalog import CATALOG


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
    updates = {"intent": resolution.intent, "intentSource": resolution.source}
    if (
        resolution.intent.is_resolved
        and resolution.intent is not IntentType.FOLLOW_UP_ANSWER
        and resolution.intent is not confirmed
    ):
        updates["confirmedConversationIntent"] = resolution.intent
    return updates
