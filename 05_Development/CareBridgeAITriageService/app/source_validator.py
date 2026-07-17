from __future__ import annotations

from urllib.parse import urlparse

from app.schemas import SourceDocument


def domain_from_url(url: str) -> str:
    return (urlparse(url).hostname or "").lower().removeprefix("www.")


def is_whitelisted_url(url: str, domain: str | None = None, approved_domains: set[str] | None = None) -> bool:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower().removeprefix("www.")
    declared_domain = (domain or "").lower().strip()
    if not approved_domains:
        return False
    if declared_domain and declared_domain not in approved_domains:
        return False
    has_specific_path = bool(parsed.path and parsed.path.strip("/"))
    return has_specific_path and any(
        host == allowed or host.endswith(f".{allowed}") for allowed in approved_domains
    )


def validate_source_domain(source: SourceDocument, approved_domains: set[str] | None = None) -> bool:
    return is_whitelisted_url(source.url, source.domain, approved_domains)


def validate_title_organization(source: SourceDocument) -> bool:
    return bool(source.title.strip() and source.organization.strip())


def validate_relevance(source: SourceDocument, symptoms: list[str]) -> bool:
    if not symptoms:
        return True
    aliases = {"breathing_difficulty": "difficulty_breathing", "dehydration": "mild_dehydration", "convulsion": "seizure", "vomiting": "persistent_vomiting"}
    source_symptoms = {aliases.get(code, code) for code in source.symptoms}
    if source_symptoms & set(symptoms):
        return True
    haystack = " ".join([
        source.title,
        source.topic,
        source.body,
    ]).lower()
    return any(symptom.replace("_", " ") in haystack or symptom in haystack for symptom in symptoms)


def validate_source(source: SourceDocument, symptoms: list[str], approved_domains: set[str] | None = None) -> bool:
    return (
        source.sourceStatus not in {"DEPRECATED", "ARCHIVED", "DRAFT"}
        and source.url.lower().startswith("https://")
        and
        validate_source_domain(source, approved_domains)
        and validate_title_organization(source)
        and validate_relevance(source, symptoms)
    )
