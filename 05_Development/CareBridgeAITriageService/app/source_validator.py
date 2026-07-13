from __future__ import annotations

from urllib.parse import urlparse

from app.config import OFFICIAL_DOMAIN_WHITELIST
from app.schemas import SourceDocument


def domain_from_url(url: str) -> str:
    return (urlparse(url).hostname or "").lower().removeprefix("www.")


def is_whitelisted_url(url: str, domain: str | None = None) -> bool:
    host = domain_from_url(url)
    declared_domain = (domain or "").lower().strip()
    if declared_domain and declared_domain not in OFFICIAL_DOMAIN_WHITELIST:
        return False
    return any(host == allowed or host.endswith(f".{allowed}") for allowed in OFFICIAL_DOMAIN_WHITELIST)


def validate_source_domain(source: SourceDocument) -> bool:
    return is_whitelisted_url(source.url, source.domain)


def validate_title_organization(source: SourceDocument) -> bool:
    return bool(source.title.strip() and source.organization.strip())


def validate_relevance(source: SourceDocument, symptoms: list[str]) -> bool:
    if not symptoms:
        return True
    source_symptoms = set(source.symptoms)
    if source_symptoms & set(symptoms):
        return True
    haystack = " ".join([
        source.title,
        source.topic,
        source.body,
    ]).lower()
    return any(symptom.replace("_", " ") in haystack or symptom in haystack for symptom in symptoms)


def validate_source(source: SourceDocument, symptoms: list[str]) -> bool:
    return (
        validate_source_domain(source)
        and validate_title_organization(source)
        and validate_relevance(source, symptoms)
    )
