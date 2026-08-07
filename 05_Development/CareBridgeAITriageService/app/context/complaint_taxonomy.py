"""Deterministic out-of-scope complaint classification.

OUT_OF_SCOPE requires **positive evidence** that a complaint belongs to another clinical
domain. An unrecognised complaint is NEEDS_MORE_INFO — "we did not understand you" is not the
same statement as "this is not our field", and conflating them would let the system dismiss
anything it failed to parse.

Every category also carries ``excludedWhenAlso``: signals that disqualify the classification.
A swollen painful leg after birth is not a musculoskeletal complaint for our purposes, and a
fall in pregnancy with bleeding is emphatically not a sports injury.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Mapping

from app.context.target_entity_resolver import _contains_any, _fold
from app.rules.condition import Presence, signal_presence

TAXONOMY_PATH = Path(__file__).resolve().parents[2] / "data" / "oos_complaint_taxonomy_v1.json"


@lru_cache(maxsize=1)
def load_taxonomy() -> Mapping[str, object]:
    return json.loads(TAXONOMY_PATH.read_text(encoding="utf-8"))


@dataclass(frozen=True)
class ComplaintClassification:
    #: ``None`` when nothing matched — which means "unknown", never "out of scope".
    category_id: str | None
    evidence: tuple[str, ...] = ()
    #: Categories that matched textually but were disqualified by a present signal.
    disqualified: tuple[str, ...] = ()

    @property
    def is_confirmed_non_reproductive(self) -> bool:
        return self.category_id is not None


def classify_complaint(
    message: str | None,
    signals: Mapping[str, object] | None = None,
) -> ComplaintClassification:
    """Classify a complaint as belonging to a known non-reproductive domain, or not at all."""

    folded = _fold(message)
    if not folded:
        return ComplaintClassification(None)

    signals = signals or {}
    disqualified: list[str] = []

    for category in load_taxonomy()["categories"]:
        hits = _contains_any(folded, category["phrases"])
        if not hits:
            continue

        blocking = [
            code for code in category.get("excludedWhenAlso", ())
            if signal_presence(signals, code) is Presence.PRESENT
        ]
        if blocking:
            # Matched the words, but a signal present in this session makes the
            # classification unsafe — e.g. bleeding alongside a reported fall.
            disqualified.append(category["categoryId"])
            continue

        return ComplaintClassification(category["categoryId"], tuple(hits), tuple(disqualified))

    return ComplaintClassification(None, (), tuple(disqualified))


def may_return_out_of_scope(
    *,
    classification: ComplaintClassification,
    safety_screen_complete: bool,
    has_reproductive_evidence: bool,
) -> bool:
    """All three policy conditions must hold before OUT_OF_SCOPE may be returned.

    The safety-screen requirement is why "đau cổ tay" is not instantly out of scope: we must
    first know the person is not also short of breath.
    """

    policy = load_taxonomy()["policy"]
    if not classification.is_confirmed_non_reproductive:
        return False
    if policy.get("requiresCompleteGlobalSafetyScreen", True) and not safety_screen_complete:
        return False
    if policy.get("requiresNoReproductiveEvidence", True) and has_reproductive_evidence:
        return False
    return True
