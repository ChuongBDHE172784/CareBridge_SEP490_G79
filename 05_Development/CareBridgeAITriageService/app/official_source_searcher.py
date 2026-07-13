from __future__ import annotations

import html
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from urllib.parse import parse_qs, quote_plus, unquote, urlparse
from urllib.request import Request, urlopen

from app.config import OFFICIAL_DOMAIN_WHITELIST
from app.schemas import SourceDocument
from app.source_validator import domain_from_url, is_whitelisted_url, validate_source


@dataclass(frozen=True)
class SearchHit:
    title: str
    url: str
    snippet: str = ""


def realtime_official_search(
    symptoms: list[str],
    matched_rules: list[str],
    max_results: int = 3,
) -> list[SourceDocument]:
    if not symptoms:
        return []

    candidates: list[SourceDocument] = []
    for query in build_search_queries(symptoms):
        for hit in _search_web(query):
            source = _source_from_hit(hit, symptoms, matched_rules)
            if source and validate_source(source, symptoms):
                candidates.append(source)
                if len(candidates) >= max_results:
                    return _dedupe(candidates)
    return _dedupe(candidates)


def build_search_queries(symptoms: list[str]) -> list[str]:
    symptom_terms = " ".join(symptom.replace("_", " ") for symptom in symptoms[:3])
    queries: list[str] = []
    for domain in sorted(OFFICIAL_DOMAIN_WHITELIST):
        queries.append(f"site:{domain} child {symptom_terms} official medical source")
    return queries


def extract_relevant_excerpt(text: str, symptoms: list[str], limit: int = 360) -> str:
    compact = " ".join(text.split())
    lower = compact.lower()
    for symptom in symptoms:
        needle = symptom.replace("_", " ").lower()
        index = lower.find(needle)
        if index >= 0:
            start = max(0, index - 120)
            return compact[start:start + limit]
    return compact[:limit]


def _search_web(query: str) -> list[SearchHit]:
    url = f"https://duckduckgo.com/html/?q={quote_plus(query)}"
    try:
        request = Request(url, headers={"User-Agent": "CareBridgeAITriage/1.0"})
        with urlopen(request, timeout=8) as response:
            body = response.read(120_000).decode("utf-8", errors="ignore")
    except Exception:
        return []

    hits: list[SearchHit] = []
    for match in re.finditer(r'<a rel="nofollow" class="result__a" href="(?P<href>[^"]+)".*?>(?P<title>.*?)</a>', body, re.S):
        title = re.sub(r"<.*?>", "", html.unescape(match.group("title"))).strip()
        href = html.unescape(match.group("href"))
        parsed = urlparse(href)
        if parsed.query:
            redirect = parse_qs(parsed.query).get("uddg", [""])[0]
            href = unquote(redirect) if redirect else href
        if is_whitelisted_url(href):
            hits.append(SearchHit(title=title, url=href))
    return hits[:5]


def _source_from_hit(hit: SearchHit, symptoms: list[str], matched_rules: list[str]) -> SourceDocument | None:
    if not is_whitelisted_url(hit.url):
        return None
    fetched_text = _fetch_page_text(hit.url)
    body = fetched_text or hit.snippet
    if not body:
        body = hit.title
    domain = _allowed_domain_for_url(hit.url)
    retrieved_at = datetime.now(timezone.utc).isoformat()
    organization = _organization_for_domain(domain)
    risk_levels = sorted({rule.split("_", 1)[0] for rule in matched_rules if "_" in rule})
    return SourceDocument(
        id=f"REALTIME_{domain.upper().replace('.', '_')}_{abs(hash(hit.url))}",
        title=hit.title,
        organization=organization,
        url=hit.url,
        domain=domain,
        lastReviewed=retrieved_at[:10],
        topic="realtime_official_search",
        ageRange="child",
        riskLevels=risk_levels,
        symptoms=symptoms,
        sourceType="official_web_page",
        sourceStatus="PENDING_REVIEW",
        retrievedAt=retrieved_at,
        retrievedBy="realtime_official_search",
        body=extract_relevant_excerpt(body, symptoms),
    )


def _fetch_page_text(url: str) -> str:
    try:
        request = Request(url, headers={"User-Agent": "CareBridgeAITriage/1.0"})
        with urlopen(request, timeout=8) as response:
            raw = response.read(80_000).decode("utf-8", errors="ignore")
    except Exception:
        return ""
    text = re.sub(r"(?is)<script.*?</script>|<style.*?</style>", " ", raw)
    text = re.sub(r"(?s)<.*?>", " ", text)
    return html.unescape(text)


def _allowed_domain_for_url(url: str) -> str:
    host = domain_from_url(url)
    for domain in OFFICIAL_DOMAIN_WHITELIST:
        if host == domain or host.endswith(f".{domain}"):
            return domain
    return host


def _organization_for_domain(domain: str) -> str:
    if domain == "who.int":
        return "WHO"
    if domain in {"moh.gov.vn", "mch.moh.gov.vn"}:
        return "Bo Y te Viet Nam"
    if domain == "cdc.gov":
        return "CDC"
    if domain == "unicef.org":
        return "UNICEF"
    if domain == "benhviennhitrunguong.gov.vn":
        return "Benh vien Nhi Trung uong"
    if domain == "nhidong.org.vn":
        return "Benh vien Nhi Dong"
    if domain == "bvndtp.org.vn":
        return "Benh vien Nhi Dong Thanh pho"
    return "Official Medical Source"


def _dedupe(sources: list[SourceDocument]) -> list[SourceDocument]:
    deduped: list[SourceDocument] = []
    seen: set[str] = set()
    for source in sources:
        if source.url not in seen:
            seen.add(source.url)
            deduped.append(source)
    return deduped
