from __future__ import annotations

import json
import time
from dataclasses import dataclass
from urllib.error import URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

from app.config import (
    EVIDENCE_REGISTRY_CACHE_SECONDS,
    EVIDENCE_REGISTRY_FAILURE_CACHE_SECONDS,
    EVIDENCE_REGISTRY_INTERNAL_KEY,
    EVIDENCE_REGISTRY_URL,
)


@dataclass(frozen=True)
class ApprovedEvidenceSource:
    id: str
    domain: str
    base_url: str
    organization: str
    applicable_stages: tuple[str, ...]


#: stage -> (cached_at, sources, was_successful). The success flag picks the TTL: a real answer
#: is held for the long TTL, an unreachable/rejected registry only for the short failure TTL.
_CACHE: dict[str, tuple[float, tuple[ApprovedEvidenceSource, ...], bool]] = {}


def _remember(
    stage: str, sources: tuple[ApprovedEvidenceSource, ...], *, succeeded: bool
) -> tuple[ApprovedEvidenceSource, ...]:
    _CACHE[stage] = (time.monotonic(), sources, succeeded)
    return sources


def approved_sources_for_stage(stage: str) -> tuple[ApprovedEvidenceSource, ...]:
    """Return only DB-approved sources for a stage, with a deliberately short TTL.

    A failed registry lookup returns no sources. It never falls back to a code
    allowlist, because serving stale/unapproved evidence is worse than partial
    evidence coverage.

    Failures are cached as well as successes. Callers ask per candidate source and ask twice
    per source, so an unreachable registry used to pay a fresh connect timeout every time —
    one RED turn measured 13 lookups and 52s against a 7s budget. Remembering the failure
    bounds that to a single attempt per failure TTL; the only cost is that citations stay
    absent for that window, and citations are post-outcome and never move a risk level.
    """
    stage = stage.upper()
    cached = _CACHE.get(stage)
    if cached is not None:
        ttl = EVIDENCE_REGISTRY_CACHE_SECONDS if cached[2] else EVIDENCE_REGISTRY_FAILURE_CACHE_SECONDS
        if time.monotonic() - cached[0] < ttl:
            return cached[1]
    if not EVIDENCE_REGISTRY_URL or not EVIDENCE_REGISTRY_INTERNAL_KEY:
        # Not a failure to remember: with nothing configured there is no call to skip, and
        # caching it would only hide a later configuration change.
        return ()
    url = f"{EVIDENCE_REGISTRY_URL}/internal/api/v1/triage/evidence-sources/approved?stage={quote(stage)}"
    request = Request(url, headers={"X-CareBridge-Internal-Key": EVIDENCE_REGISTRY_INTERNAL_KEY})
    try:
        with urlopen(request, timeout=2.0) as response:
            if response.status != 200:
                return _remember(stage, (), succeeded=False)
            payload = json.loads(response.read().decode("utf-8"))
    except (URLError, OSError, ValueError, TimeoutError):
        return _remember(stage, (), succeeded=False)
    if not isinstance(payload, dict) or payload.get("success") is not True or not isinstance(payload.get("data"), list):
        return _remember(stage, (), succeeded=False)
    sources: list[ApprovedEvidenceSource] = []
    for item in payload["data"]:
        if not isinstance(item, dict):
            continue
        domain = str(item.get("domain") or "").strip().lower().removeprefix("www.")
        base_url = str(item.get("baseUrl") or "").strip()
        if not domain or not base_url.startswith("https://"):
            continue
        stages = tuple(str(value).upper() for value in item.get("applicableStages", []) if value)
        if stage not in stages:
            continue
        sources.append(ApprovedEvidenceSource(
            id=str(item.get("id") or domain), domain=domain, base_url=base_url,
            organization=str(item.get("organization") or domain), applicable_stages=stages,
        ))
    return _remember(stage, tuple(sources), succeeded=True)


def clear_registry_cache() -> None:
    _CACHE.clear()
