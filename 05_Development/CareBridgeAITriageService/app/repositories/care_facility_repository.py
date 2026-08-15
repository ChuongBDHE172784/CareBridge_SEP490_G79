"""Care facilities, read from the version-controlled seed file.

No runtime call to Places, Overpass or any other provider. The seed is small, checked in, and
reviewable in a diff, which is what a list of hospitals shown to someone in an emergency should
be.

A missing or malformed seed returns an empty list rather than raising. The hotlines are the part
of the response that actually matters when something is wrong, and they do not depend on this
file — see the disclaimer suffix the service appends when the list comes back empty.
"""

from __future__ import annotations

import json
from pathlib import Path

_FACILITIES_PATH = Path(__file__).resolve().parents[2] / "data" / "care_facilities.json"


def load_facilities() -> list[dict]:
    """Every seeded facility, whatever its phone verification state."""

    try:
        with open(_FACILITIES_PATH, encoding="utf-8") as handle:
            loaded = json.load(handle)
    except (OSError, json.JSONDecodeError):
        return []
    return loaded if isinstance(loaded, list) else []


def load_verified_facilities() -> list[dict]:
    """Only facilities whose phone number has been checked against an official page.

    Empty in the current seed: all five entries are PENDING because the 2026-08-15 crawl could
    not extract a specific number for any of them, and showing an unverified number to someone
    calling in an emergency is worse than showing none.
    """

    return [
        facility
        for facility in load_facilities()
        if facility.get("phoneVerificationStatus") == "VERIFIED"
    ]
