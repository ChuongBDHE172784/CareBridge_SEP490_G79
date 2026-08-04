from __future__ import annotations

import hashlib
import html
import json
import re
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen

from app.config import (
    EVIDENCE_CACHE_TTL_DAYS,
    GOOGLE_SEARCH_API_KEY,
    GOOGLE_SEARCH_ENGINE_ID,
    REALTIME_SEARCH_TIMEOUT_SECONDS,
)
from app.evidence_registry_client import approved_sources_for_stage
from app.schemas import SourceDocument
from app.source_validator import domain_from_url, is_whitelisted_url, validate_source


@dataclass(frozen=True)
class SearchHit:
    title: str
    url: str
    snippet: str = ""


_SEARCH_CACHE: dict[
    tuple[str, tuple[str, ...], tuple[str, ...], int, tuple[str, ...]],
    tuple[float, list[SourceDocument]],
] = {}

_SYMPTOM_TERMS: dict[str, tuple[str, ...]] = {
    "fever": ("fever", "high temperature", "sốt"),
    "high_fever": ("high fever", "high temperature", "sốt cao"),
    "cough": ("cough", "ho"),
    "runny_nose": ("runny nose", "sổ mũi"),
    "difficulty_breathing": ("difficulty breathing", "breathing difficulty", "khó thở"),
    "chest_indrawing": ("chest indrawing", "chest retraction", "rút lõm"),
    "cyanosis": ("cyanosis", "blue lips", "tím tái"),
    "seizure": ("seizure", "convulsion", "co giật"),
    "lethargy": ("lethargy", "lethargic", "li bì"),
    "difficult_to_wake": ("difficult to wake", "unconscious", "khó đánh thức"),
    "unable_to_drink": ("unable to drink", "unable to breastfeed", "không uống"),
    "poor_feeding": ("poor feeding", "feeding poorly", "bỏ bú"),
    "vomiting": ("vomiting", "vomit", "nôn"),
    "persistent_vomiting": ("persistent vomiting", "vomits everything", "nôn liên tục"),
    "diarrhea": ("diarrhea", "diarrhoea", "tiêu chảy"),
    "mild_dehydration": ("dehydration", "mild dehydration", "mất nước"),
    "severe_dehydration": ("severe dehydration", "sunken eyes", "mất nước nặng"),
    "rash": ("rash", "phát ban"),
    "worsening_condition": ("worsening", "condition gets worse", "nặng hơn"),
}


def realtime_official_search(
    symptoms: list[str],
    matched_rules: list[str],
    stage: str = "INFANT",
    max_results: int = 3,
    request_deadline: float | None = None,
) -> list[SourceDocument]:
    if not symptoms:
        return []

    domains = {source.domain for source in approved_sources_for_stage(stage)}
    if not domains:
        return []

    # Stage AND the approved-domain set are part of the key: the same symptom
    # codes mean different search terms and a different validated domain set for
    # maternal vs pediatric stages (see build_search_queries), so a cached
    # pediatric hit must never be served back for a maternal request with the
    # same normalized symptom codes — and a newly whitelisted/revoked domain
    # must not be masked by a stale cache entry keyed only on symptoms/stage.
    cache_key = (
        stage.upper(), tuple(sorted(symptoms)), tuple(sorted(matched_rules)),
        max_results, tuple(sorted(domains)),
    )
    cached = _SEARCH_CACHE.get(cache_key)
    ttl_seconds = EVIDENCE_CACHE_TTL_DAYS * 24 * 60 * 60
    if cached and time.monotonic() - cached[0] < ttl_seconds:
        return [source.model_copy(deep=True) for source in cached[1]]

    deadline = min(
        request_deadline if request_deadline is not None else float("inf"),
        time.monotonic() + REALTIME_SEARCH_TIMEOUT_SECONDS,
    )
    candidates: list[SourceDocument] = []
    for query in build_search_queries(symptoms, domains, stage):
        if _remaining(deadline) <= 0:
            break
        for hit in _search_web(query, deadline):
            if _remaining(deadline) <= 0:
                break
            source = _source_from_hit(hit, symptoms, matched_rules, deadline, domains)
            if source and validate_source(source, symptoms, domains):
                candidates.append(source)
                if len(candidates) >= max_results:
                    result = _dedupe(candidates)
                    _SEARCH_CACHE[cache_key] = (time.monotonic(), result)
                    return [source.model_copy(deep=True) for source in result]
    result = _dedupe(candidates)
    # A transient search failure must not suppress evidence retrieval for 30 days.
    if result:
        _SEARCH_CACHE[cache_key] = (time.monotonic(), result)
    return [source.model_copy(deep=True) for source in result]


_MATERNAL_SUBJECT_TERMS: dict[str, str] = {
    "PRECONCEPTION": "woman planning pregnancy",
    "PREGNANCY": "pregnant woman",
    "POSTPARTUM": "postpartum woman",
}


def build_search_queries(
    symptoms: list[str], approved_domains: set[str] | None = None, stage: str = "INFANT",
) -> list[str]:
    symptom_terms = " ".join(symptom.replace("_", " ") for symptom in symptoms[:3])
    # The user is describing her own symptoms at a specific life stage, not a
    # child's — searching "child <symptom>" (or the wrong maternal stage) returns
    # pages that don't match what she actually reported.
    subject_term = _MATERNAL_SUBJECT_TERMS.get(stage.upper(), "child")
    return [
        f"site:{domain} {subject_term} {symptom_terms} official medical source"
        for domain in sorted(approved_domains or set())
    ]


def extract_relevant_excerpt(
    text: str, symptoms: list[str], limit: int = 360,
) -> tuple[str, list[str]] | None:
    compact = " ".join(text.split())
    lower = compact.lower()
    for symptom in symptoms:
        for term in _SYMPTOM_TERMS.get(symptom, (symptom.replace("_", " "),)):
            match = re.search(rf"(?<!\w){re.escape(term.lower())}(?!\w)", lower)
            if match:
                start = max(0, match.start() - 120)
                return compact[start:start + limit], [symptom]
    return None


def _search_web(query: str, deadline: float | None = None) -> list[SearchHit]:
    deadline = deadline or (time.monotonic() + REALTIME_SEARCH_TIMEOUT_SECONDS)
    if _remaining(deadline) <= 0:
        return []
    # Scraping a general search engine's HTML results page is not used here: it
    # is unreliable behind bot-detection/CAPTCHA challenges, and working around
    # that would violate this service's "no CAPTCHA bypass" policy. A configured
    # search API is required instead; with none configured, realtime search
    # simply contributes no results (local Knowledge Base citations still work).
    if not GOOGLE_SEARCH_API_KEY or not GOOGLE_SEARCH_ENGINE_ID:
        return []
    params = urlencode({
        "key": GOOGLE_SEARCH_API_KEY,
        "cx": GOOGLE_SEARCH_ENGINE_ID,
        "q": query,
        "num": 5,
    })
    url = f"https://www.googleapis.com/customsearch/v1?{params}"
    try:
        request = Request(url, headers={"User-Agent": "CareBridgeAITriage/1.0"})
        with urlopen(request, timeout=_io_timeout(deadline)) as response:
            body = response.read(120_000).decode("utf-8", errors="ignore")
        payload = json.loads(body)
    except Exception:
        return []

    hits: list[SearchHit] = []
    items = payload.get("items") if isinstance(payload, dict) else None
    if not isinstance(items, list):
        items = []
    for item in items:
        if not isinstance(item, dict):
            continue
        link = item.get("link")
        title = item.get("title")
        if not isinstance(link, str) or not isinstance(title, str):
            continue
        if urlparse(link).scheme != "https":
            continue
        hits.append(SearchHit(title=title.strip(), url=link, snippet=str(item.get("snippet") or "")))
    return hits[:5]


def _source_from_hit(
    hit: SearchHit, symptoms: list[str], matched_rules: list[str], deadline: float | None = None,
    approved_domains: set[str] | None = None,
) -> SourceDocument | None:
    if not is_whitelisted_url(hit.url, approved_domains=approved_domains):
        return None
    fetched = _fetch_page(hit.url, deadline, approved_domains)
    if fetched is None:
        return None
    final_url, page_title, fetched_text, etag, last_modified = fetched
    if not is_whitelisted_url(final_url, approved_domains=approved_domains) or not fetched_text or not page_title:
        return None
    support = extract_relevant_excerpt(f"{page_title} {fetched_text}", symptoms)
    if support is None:
        return None
    excerpt, supported_symptoms = support
    domain = _allowed_domain_for_url(final_url, approved_domains or set())
    retrieved_at = datetime.now(timezone.utc).isoformat()
    organization = _organization_for_domain(domain)
    risk_levels = sorted({rule.split("_", 1)[0] for rule in matched_rules if "_" in rule})
    stable_id = hashlib.sha256(final_url.encode("utf-8")).hexdigest()[:20].upper()
    return SourceDocument(
        id=f"REALTIME_{domain.upper().replace('.', '_')}_{stable_id}",
        title=page_title,
        organization=organization,
        url=final_url,
        domain=domain,
        lastReviewed=retrieved_at[:10],
        topic="realtime_official_search",
        ageRange="child",
        riskLevels=risk_levels,
        symptoms=supported_symptoms,
        sourceType="official_web_page",
        sourceStatus="PENDING_REVIEW",
        sourceVersion=f"realtime-{retrieved_at[:10]}",
        section="Exact supporting passage",
        retrievedAt=retrieved_at,
        retrievedBy="realtime_official_search",
        etag=etag,
        lastModified=last_modified,
        body=excerpt,
    )


def _fetch_page(
    url: str, deadline: float | None = None, approved_domains: set[str] | None = None,
) -> tuple[str, str, str, str | None, str | None] | None:
    deadline = deadline or (time.monotonic() + REALTIME_SEARCH_TIMEOUT_SECONDS)
    if _remaining(deadline) <= 0:
        return None
    try:
        request = Request(url, headers={"User-Agent": "CareBridgeAITriage/1.0"})
        with urlopen(request, timeout=_io_timeout(deadline)) as response:
            if getattr(response, "status", 200) != 200:
                return None
            content_type = response.headers.get("Content-Type", "").lower()
            if not any(value in content_type for value in ("text/html", "text/plain", "application/xhtml+xml")):
                return None
            final_url = response.geturl()
            if not is_whitelisted_url(final_url, approved_domains=approved_domains):
                return None
            etag = response.headers.get("ETag")
            last_modified = response.headers.get("Last-Modified")
            raw = response.read(80_000).decode("utf-8", errors="ignore")
    except Exception:
        return None
    title_match = re.search(r"(?is)<title[^>]*>(.*?)</title>", raw)
    page_title = re.sub(r"<.*?>", "", html.unescape(title_match.group(1))).strip() if title_match else ""
    text = re.sub(r"(?is)<script.*?</script>|<style.*?</style>", " ", raw)
    text = re.sub(r"(?s)<.*?>", " ", text)
    return final_url, page_title, html.unescape(text), etag, last_modified


def _io_timeout(deadline: float) -> float:
    return max(0.05, min(REALTIME_SEARCH_TIMEOUT_SECONDS, _remaining(deadline)))


def _remaining(deadline: float) -> float:
    return deadline - time.monotonic()


def _allowed_domain_for_url(url: str, approved_domains: set[str]) -> str:
    host = domain_from_url(url)
    for domain in approved_domains:
        if host == domain or host.endswith(f".{domain}"):
            return domain
    return host


def _organization_for_domain(domain: str) -> str:
    for source in approved_sources_for_stage("INFANT"):
        if source.domain == domain:
            return source.organization
    return "Official Medical Source"


def _dedupe(sources: list[SourceDocument]) -> list[SourceDocument]:
    deduped: list[SourceDocument] = []
    seen: set[str] = set()
    for source in sources:
        if source.url not in seen:
            seen.add(source.url)
            deduped.append(source)
    return deduped
