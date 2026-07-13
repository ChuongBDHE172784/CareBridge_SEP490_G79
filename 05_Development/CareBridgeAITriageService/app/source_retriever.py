import json
from datetime import datetime, timezone
from urllib.parse import urlparse

import frontmatter

from app.config import (
    LEGAL_SAFETY_NOTE,
    MEDICAL_SOURCES_DIR,
    OFFICIAL_DOMAIN_WHITELIST,
    RULE_SOURCE_MAPPING_FILE,
)
from app.schemas import Citation, Evidence, SourceDocument
from app.source_validator import validate_source
from app import official_source_searcher


def load_sources() -> list[SourceDocument]:
    if not MEDICAL_SOURCES_DIR.exists():
        return []
    sources: list[SourceDocument] = []
    for path in sorted(MEDICAL_SOURCES_DIR.glob("*.md")):
        post = frontmatter.load(path)
        sources.append(
            SourceDocument(
                id=post.metadata.get("id", path.stem),
                title=post.metadata.get("title", path.stem),
                organization=post.metadata.get("organization", "Hospital Guideline"),
                url=post.metadata.get("url", ""),
                domain=post.metadata.get("domain", ""),
                lastReviewed=str(post.metadata.get("lastReviewed", "")),
                topic=post.metadata.get("topic", ""),
                ageRange=post.metadata.get("ageRange", ""),
                riskLevels=list(post.metadata.get("riskLevels", [])),
                symptoms=list(post.metadata.get("symptoms", [])),
                sourceType=post.metadata.get("sourceType", "official_guideline"),
                sourceStatus=post.metadata.get("sourceStatus", "REVIEWED"),
                retrievedAt=str(post.metadata.get("retrievedAt", "")) or None,
                retrievedBy=post.metadata.get("retrievedBy"),
                body=post.content.strip(),
            )
        )
    return [source for source in sources if is_whitelisted_source(source)]


def retrieve_sources(normalized_symptoms: list[str], matched_rules: list[str]) -> list[SourceDocument]:
    sources = load_sources()
    source_by_id = {source.id: source for source in sources}
    mapped_source_ids = _source_ids_for_rules(matched_rules)
    mapped_sources = [
        source_by_id[source_id]
        for source_id in mapped_source_ids
        if source_id in source_by_id and _matches(source_by_id[source_id], normalized_symptoms, matched_rules)
    ]
    fallback_sources = [
        source for source in sources
        if source.id not in mapped_source_ids and _matches(source, normalized_symptoms, matched_rules)
    ]
    combined = mapped_sources + fallback_sources
    deduped: list[SourceDocument] = []
    seen: set[str] = set()
    for source in combined:
        if source.id not in seen:
            seen.add(source.id)
            deduped.append(source)
    return deduped[:4]


def retrieve_realtime_sources(normalized_symptoms: list[str], matched_rules: list[str]) -> list[SourceDocument]:
    return [
        source
        for source in official_source_searcher.realtime_official_search(normalized_symptoms, matched_rules)
        if validate_source(source, normalized_symptoms)
    ][:4]


def attach_citations(
    sources: list[SourceDocument],
    normalized_symptoms: list[str] | None = None,
    matched_rules: list[str] | None = None,
) -> list[Citation]:
    normalized_symptoms = normalized_symptoms or []
    matched_rules = matched_rules or []
    retrieved_at = datetime.now(timezone.utc).isoformat()
    citations: list[Citation] = []
    for source in sources:
        if not is_whitelisted_source(source):
            continue
        excerpt = " ".join(source.body.split())[:240]
        citations.append(
            Citation(
                id=source.id,
                title=source.title,
                source=source.organization,
                organization=source.organization,
                url=source.url,
                domain=source.domain,
                section=source.topic,
                excerpt=excerpt,
                retrievedAt=source.retrievedAt or retrieved_at,
                matchedSymptoms=_matched_symptoms(source, normalized_symptoms),
                matchedRules=[
                    rule for rule in matched_rules
                    if rule.split("_", 1)[0] in source.riskLevels or set(source.symptoms) & set(normalized_symptoms)
                ],
                confidence=0.95 if source.sourceStatus == "REVIEWED" else 0.75,
                sourceStatus=source.sourceStatus,
                lastReviewed=source.lastReviewed,
            )
        )
    return citations


def build_evidence(citations: list[Citation], normalized_symptoms: list[str]) -> Evidence:
    matched_symptoms = sorted({
        symptom
        for citation in citations
        for symptom in citation.matchedSymptoms
    })
    return Evidence(
        legalSafetyNote=LEGAL_SAFETY_NOTE,
        matchedSymptoms=matched_symptoms,
        matchedOfficialSources=[
            citation.id or citation.title
            for citation in citations
        ],
        unmatchedSymptoms=[
            symptom for symptom in normalized_symptoms
            if symptom not in matched_symptoms
        ],
    )


def is_whitelisted_source(source: SourceDocument) -> bool:
    domain = (source.domain or "").lower().strip()
    host = urlparse(source.url).hostname or ""
    host = host.lower().removeprefix("www.")
    if domain and domain in OFFICIAL_DOMAIN_WHITELIST and (not host or host == domain or host.endswith(f".{domain}")):
        return True
    return any(host == allowed or host.endswith(f".{allowed}") for allowed in OFFICIAL_DOMAIN_WHITELIST)


def _source_ids_for_rules(matched_rules: list[str]) -> list[str]:
    if not RULE_SOURCE_MAPPING_FILE.exists():
        return []
    mappings = json.loads(RULE_SOURCE_MAPPING_FILE.read_text(encoding="utf-8"))
    source_ids: list[str] = []
    for rule in matched_rules:
        source_ids.extend(mappings.get(rule, {}).get("requiredSources", []))
    return source_ids


def _matches(source: SourceDocument, symptoms: list[str], rules: list[str]) -> bool:
    if rules and source.riskLevels:
        has_risk_match = any(rule.split("_", 1)[0] in source.riskLevels for rule in rules)
        has_symptom_match = bool(set(symptoms) & set(source.symptoms))
        if has_risk_match and has_symptom_match:
            return True
    if set(symptoms) & set(source.symptoms):
        return True
    topic = source.topic.lower()
    if "fever" in symptoms and topic == "fever":
        return True
    if {"cough", "runny_nose", "breathing_difficulty", "cyanosis"} & set(symptoms) and topic == "respiratory":
        return True
    if {"diarrhea", "dehydration"} & set(symptoms) and "diarrhea" in topic:
        return True
    return any(rule.startswith("RED_") and topic == "danger_signs" for rule in rules)


def _matched_symptoms(source: SourceDocument, normalized_symptoms: list[str]) -> list[str]:
    return sorted(set(source.symptoms) & set(normalized_symptoms))
