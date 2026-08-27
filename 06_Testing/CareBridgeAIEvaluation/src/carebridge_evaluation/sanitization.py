"""Minimize API/runtime responses before they are persisted in evaluation reports."""

from __future__ import annotations

from typing import Any

SENSITIVE_KEYS = {
    "address",
    "babyprofileid",
    "email",
    "intakesessionid",
    "messages",
    "motherprofileid",
    "name",
    "parentfreetext",
    "phonenumber",
    "userid",
}


def minimize_response(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            key: "[REDACTED]" if key.lower() in SENSITIVE_KEYS else minimize_response(item)
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [minimize_response(item) for item in value]
    return value

