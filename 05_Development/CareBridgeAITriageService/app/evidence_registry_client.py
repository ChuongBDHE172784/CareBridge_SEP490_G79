from __future__ import annotations

import json
import time
from dataclasses import dataclass
from urllib.error import URLError
from urllib.parse import quote
from urllib.request import Request, urlopen

from app.config import (
    EVIDENCE_REGISTRY_CACHE_SECONDS,
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


_CACHE: dict[str, tuple[float, tuple[ApprovedEvidenceSource, ...]]] = {}


def approved_sources_for_stage(stage: str) -> tuple[ApprovedEvidenceSource, ...]:
    """Return only DB-approved sources for a stage, with a deliberately short TTL.

    A failed registry lookup returns no sources. It never falls back to a code
    allowlist, because serving stale/unapproved evidence is worse than partial
    evidence coverage.
    """
    stage = stage.upper()
    cached = _CACHE.get(stage)
    if cached and time.monotonic() - cached[0] < EVIDENCE_REGISTRY_CACHE_SECONDS:
        return cached[1]
    if not EVIDENCE_REGISTRY_URL or not EVIDENCE_REGISTRY_INTERNAL_KEY:
        return ()
    url = f"{EVIDENCE_REGISTRY_URL}/internal/api/v1/triage/evidence-sources/approved?stage={quote(stage)}"
    request = Request(url, headers={"X-CareBridge-Internal-Key": EVIDENCE_REGISTRY_INTERNAL_KEY})
    try:
        with urlopen(request, timeout=2.0) as response:
            if response.status != 200:
                return ()
            payload = json.loads(response.read().decode("utf-8"))
    except (URLError, OSError, ValueError, TimeoutError):
        return ()
    if not isinstance(payload, dict) or payload.get("success") is not True or not isinstance(payload.get("data"), list):
        return ()
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
    frozen = tuple(sources)
    _CACHE[stage] = (time.monotonic(), frozen)
    return frozen


def clear_registry_cache() -> None:
    _CACHE.clear()
