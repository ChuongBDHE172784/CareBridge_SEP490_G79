"""V2 graph adapter for deterministic intent resolution."""

from __future__ import annotations

from functools import lru_cache
from typing import Mapping

from app.context.intent_resolver import resolve_intent
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
    submitted = [message] if message in _fixed_option_codes() else None
    resolution = resolve_intent(
        latest_user_message=message,
        submitted_option_codes=submitted,
    )
    return {"intent": resolution.intent, "intentSource": resolution.source}
