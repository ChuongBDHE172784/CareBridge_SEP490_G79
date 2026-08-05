import json
import re
from typing import Optional
from datetime import datetime, timezone
from urllib.parse import urlparse

import frontmatter

from app.config import (
    LEGAL_SAFETY_NOTE,
    MEDICAL_SOURCES_DIR,
    RULE_SOURCE_MAPPING_FILE,
)
from app.evidence_registry_client import approved_sources_for_stage
from app.risk_rules import MATERNAL_STAGES
from app.schemas import Citation, Evidence, SourceDocument
from app.source_validator import validate_source
from app import official_source_searcher


_AGE_RANGE_PATTERN = re.compile(r"^(\d+)\s*-\s*(\d+)\s*(months|years)$", re.IGNORECASE)


def load_sources(stage: str = "INFANT", child_age_months: int | None = None) -> list[SourceDocument]:
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
                applicableStages=list(post.metadata.get("applicableStages", ["INFANT", "TODDLER"])),
                sourceType=post.metadata.get("sourceType", "official_guideline"),
                sourceStatus=post.metadata.get("sourceStatus", "DRAFT"),
                sourceVersion=str(post.metadata.get("sourceVersion", "1.0")),
                approvedAt=str(post.metadata.get("approvedAt", post.metadata.get("lastReviewed", ""))) or None,
                approvedBy=post.metadata.get("approvedBy", "CareBridge clinical review"),
                deprecatedAt=str(post.metadata.get("deprecatedAt", "")) or None,
                section=post.metadata.get("section", post.metadata.get("topic")),
                retrievedAt=str(post.metadata.get("retrievedAt", "")) or None,
                retrievedBy=post.metadata.get("retrievedBy"),
                body=post.content.strip(),
            )
        )
    return [
        source for source in sources
        if is_approved_source(source, stage)
        and source.sourceStatus == "APPROVED"
        and stage in source.applicableStages
        # Mothers have no childAgeMonths; the age-range gate only makes sense for
        # pediatric stages. Maternal applicability is decided by applicableStages above.
        and (stage in MATERNAL_STAGES or _applies_to_age(source, child_age_months))
    ]


def retrieve_sources(
    normalized_symptoms: list[str], matched_rules: list[str], stage: str = "INFANT",
    child_age_months: int | None = None,
) -> list[SourceDocument]:
    sources = load_sources(stage, child_age_months)
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
    seen_urls: set[str] = set()
    for source in combined:
        if source.url not in seen_urls:
            seen_urls.add(source.url)
            deduped.append(source)
    return deduped[:4]


def retrieve_realtime_sources(
    normalized_symptoms: list[str],
    matched_rules: list[str],
    stage: str = "INFANT",
    request_deadline: Optional[float] = None,
) -> list[SourceDocument]:
    return [
        source
        for source in official_source_searcher.realtime_official_search(
            normalized_symptoms, matched_rules, stage, request_deadline=request_deadline
        )
        if is_approved_source(source, stage) and validate_source(source, normalized_symptoms, approved_domains(stage))
    ][:4]


def attach_citations(
    sources: list[SourceDocument],
    normalized_symptoms: list[str] | None = None,
    matched_rules: list[str] | None = None,
    stage: str = "INFANT",
) -> list[Citation]:
    normalized_symptoms = normalized_symptoms or []
    matched_rules = matched_rules or []
    retrieved_at = datetime.now(timezone.utc).isoformat()
    citations: list[Citation] = []
    for source in sources:
        if not is_approved_source(source, stage) or source.sourceStatus in {"DRAFT", "DEPRECATED", "ARCHIVED"}:
            continue
        # PENDING_REVIEW is intentionally allowed through, but only when it came
        # from realtime_official_search (see official_source_searcher._source_from_hit),
        # which by this point has already passed domain whitelisting + relevance
        # validation. This is deliberately narrower than "any PENDING_REVIEW source"
        # so a future, differently-sourced PENDING_REVIEW entry does not silently
        # become user-facing without going through that same validation.
        if source.sourceStatus == "PENDING_REVIEW" and source.retrievedBy != "realtime_official_search":
            continue
        excerpt = " ".join(source.body.split())[:240]
        source_matched_symptoms = _matched_symptoms(source, normalized_symptoms)
        source_matched_rules = [
            rule for rule in matched_rules
            if rule.split("_", 1)[0] in source.riskLevels and source_matched_symptoms
        ]
        citations.append(
            Citation(
                id=source.id,
                sourceId=source.id,
                title=source.title,
                source=source.organization,
                organization=source.organization,
                url=source.url,
                domain=source.domain,
                section=source.section or source.topic,
                heading=source.section or source.topic,
                excerpt=excerpt,
                retrievedAt=source.retrievedAt or retrieved_at,
                matchedSymptoms=source_matched_symptoms,
                matchedRules=source_matched_rules,
                sourceVersion=source.sourceVersion,
                sourceStatus=source.sourceStatus,
                retrievalMode="REALTIME" if source.sourceStatus == "PENDING_REVIEW" else "LOCAL",
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


def approved_domains(stage: str) -> set[str]:
    return {source.domain for source in approved_sources_for_stage(stage)}


def is_approved_source(source: SourceDocument, stage: str) -> bool:
    domain = (source.domain or "").lower().strip()
    host = urlparse(source.url).hostname or ""
    host = host.lower().removeprefix("www.")
    domains = approved_domains(stage)
    if domain and domain in domains and (not host or host == domain or host.endswith(f".{domain}")):
        return True
    return any(host == allowed or host.endswith(f".{allowed}") for allowed in domains)


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
    if {"difficulty_breathing", "chest_indrawing", "cyanosis"} & set(symptoms) and topic == "respiratory":
        return True
    if {"diarrhea", "mild_dehydration", "severe_dehydration"} & set(symptoms) and "diarrhea" in topic:
        return True
    return any(rule.startswith("RED_") and topic == "danger_signs" for rule in rules)


def _matched_symptoms(source: SourceDocument, normalized_symptoms: list[str]) -> list[str]:
    aliases = {
        "breathing_difficulty": "difficulty_breathing",
        "dehydration": "mild_dehydration",
        "convulsion": "seizure",
    }
    source_codes = {aliases.get(code, code) for code in source.symptoms}
    return sorted(source_codes & set(normalized_symptoms))


def _applies_to_age(source: SourceDocument, child_age_months: int | None) -> bool:
    if child_age_months is None or child_age_months < 0:
        return False
    match = _AGE_RANGE_PATTERN.fullmatch(source.ageRange.strip())
    if not match:
        return False
    minimum = int(match.group(1))
    maximum = int(match.group(2))
    if match.group(3).lower() == "years":
        minimum *= 12
        maximum *= 12
    return minimum <= child_age_months <= maximum
