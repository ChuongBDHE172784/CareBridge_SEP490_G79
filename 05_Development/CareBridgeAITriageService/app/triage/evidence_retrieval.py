"""Local, post-outcome retrieval over cryptographically verified source documents."""

from __future__ import annotations

from collections import Counter
from collections.abc import Callable
from dataclasses import dataclass
from hashlib import sha256
from math import log
from pathlib import Path
import re
import unicodedata
from urllib.parse import urlparse

import frontmatter
import yaml

from app.config import MEDICAL_SOURCES_DIR


_HASH = re.compile(r"^[0-9a-f]{64}$")


@dataclass(frozen=True)
class VerifiedDocument:
    source_id: str
    title: str
    organization: str
    publisher: str
    url: str
    domain: str
    section: str
    content_hash: str
    language: str
    target_entities: tuple[str, ...]
    applicable_stages: tuple[str, ...]
    rule_ids: tuple[str, ...]
    body: str


def load_verified_corpus(
    *, corpus_dir: Path = MEDICAL_SOURCES_DIR, allowed_domains: set[str], target: str,
    stage: str, language: str = "vi", rule_ids: tuple[str, ...] = (),
    on_reject: Callable[[str], None] | None = None,
) -> list[VerifiedDocument]:
    """Load only SOURCE_VERIFIED documents whose body still matches its declared hash."""

    if not corpus_dir.exists() or not allowed_domains:
        return []
    documents: list[VerifiedDocument] = []
    for path in sorted(corpus_dir.glob("*.md")):
        try:
            post = frontmatter.load(path)
            metadata = post.metadata
            body = post.content.strip()
            domain = str(metadata.get("domain") or "").lower().removeprefix("www.")
            url = str(metadata.get("url") or "")
            section = str(metadata.get("sectionOrPage") or metadata.get("section") or "").strip()
            declared_hash = str(metadata.get("contentHash") or "").lower()
            if not _verified_metadata(
                metadata, body, domain, url, section, declared_hash, allowed_domains
            ):
                if on_reject is not None:
                    on_reject("CITATION_REJECTED")
                continue
            targets = tuple(str(item) for item in metadata.get("targetEntities", ()))
            stages = tuple(_canonical_stage(str(item)) for item in metadata.get("applicableStages", ()))
            languages = tuple(str(item) for item in metadata.get("languages", (metadata.get("language", "vi"),)))
            mapped_rules = tuple(str(item) for item in metadata.get("ruleIds", ()))
            if target not in targets or _canonical_stage(stage) not in stages or language not in languages:
                continue
            if rule_ids and (not mapped_rules or not set(rule_ids) & set(mapped_rules)):
                continue
            documents.append(VerifiedDocument(
                source_id=str(metadata.get("id") or path.stem),
                title=str(metadata.get("title") or path.stem),
                organization=str(metadata.get("organization") or ""),
                publisher=str(metadata.get("publisher") or metadata.get("organization") or ""),
                url=url, domain=domain, section=section, content_hash=declared_hash,
                language=language, target_entities=targets, applicable_stages=stages,
                rule_ids=mapped_rules, body=body,
            ))
        except (OSError, TypeError, ValueError, yaml.YAMLError):
            if on_reject is not None:
                on_reject("CITATION_REJECTED")
            continue
    return documents


def retrieve_verified_evidence(
    state: dict[str, object], *, allowed_domains: set[str], corpus_dir: Path = MEDICAL_SOURCES_DIR,
    limit: int = 4, on_reject: Callable[[str], None] | None = None,
) -> list[dict[str, object]]:
    """Retrieve after a deterministic result; retrieval can never modify that result."""

    if state.get("triageOutcome") not in {"RED", "YELLOW"}:
        return []
    target = _value(state.get("targetEntity"))
    stage = _value(state.get("stage"))
    rule_ids = tuple(str(item) for item in state.get("decisiveRuleIds", []) if item)
    documents = load_verified_corpus(
        corpus_dir=corpus_dir, allowed_domains=allowed_domains, target=target,
        stage=stage, language="vi", rule_ids=rule_ids, on_reject=on_reject,
    )
    query = " ".join([
        *rule_ids,
        *(str(item) for item in state.get("reasonCodes", []) if item),
        *(str(code) for code, observation in (state.get("signals") or {}).items()
          if _present(observation)),
    ])
    ranked = sorted(
        bm25_rank(documents, query),
        # Clinical relevance is authoritative. Vietnamese provenance is only a stable
        # tie-breaker between equally relevant, already-verified documents.
        key=lambda item: (-item[1], -_vietnam_priority(item[0]), item[0].source_id),
    )
    selected: list[tuple[VerifiedDocument, float]] = []
    who_count = 0
    for document, score in ranked:
        if score <= 0:
            continue
        if _is_who(document):
            if who_count >= 1:
                continue
            who_count += 1
        selected.append((document, score))
        if len(selected) >= limit:
            break
    return [
        {
            "sourceId": document.source_id,
            "title": document.title,
            "organization": document.organization,
            "publisher": document.publisher,
            "url": document.url,
            "domain": document.domain,
            "section": document.section,
            "contentHash": document.content_hash,
            "sourceStatus": "SOURCE_VERIFIED",
            "retrievalMode": "LOCAL_BM25",
            "ruleIds": list(document.rule_ids),
        }
        for document, _score in selected
    ]


def bm25_rank(documents: list[VerifiedDocument], query: str) -> list[tuple[VerifiedDocument, float]]:
    if not documents:
        return []
    query_terms = _tokens(query)
    if not query_terms:
        return []
    tokenized = [_tokens(" ".join((doc.title, doc.section, doc.body, *doc.rule_ids))) for doc in documents]
    average_length = sum(map(len, tokenized)) / len(tokenized) or 1
    document_frequency = Counter(term for term in set(query_terms)
                                 for tokens in tokenized if term in tokens)
    scored: list[tuple[VerifiedDocument, float]] = []
    for document, tokens in zip(documents, tokenized):
        counts = Counter(tokens)
        score = 0.0
        for term in query_terms:
            frequency = counts[term]
            if not frequency:
                continue
            inverse = log(1 + (len(documents) - document_frequency[term] + 0.5)
                          / (document_frequency[term] + 0.5))
            denominator = frequency + 1.5 * (1 - 0.75 + 0.75 * len(tokens) / average_length)
            score += inverse * frequency * 2.5 / denominator
        scored.append((document, score))
    return sorted(scored, key=lambda item: (-item[1], item[0].source_id))


def corpus_inventory(corpus_dir: Path = MEDICAL_SOURCES_DIR) -> dict[str, int]:
    total = verified = 0
    for path in corpus_dir.glob("*.md") if corpus_dir.exists() else ():
        total += 1
        try:
            if frontmatter.load(path).metadata.get("sourceStatus") == "SOURCE_VERIFIED":
                verified += 1
        except OSError:
            pass
    return {"documents": total, "declaredSourceVerified": verified}


def _verified_metadata(metadata, body, domain, url, section, declared_hash, allowed_domains) -> bool:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower().removeprefix("www.")
    return (
        metadata.get("sourceStatus") == "SOURCE_VERIFIED"
        and metadata.get("publisherTrustStatus") == "DOMAIN_AND_DOCUMENT_VERIFIED"
        and domain in allowed_domains
        and (host == domain or host.endswith(f".{domain}"))
        and parsed.scheme == "https" and bool(parsed.path.strip("/"))
        and bool(section) and bool(str(metadata.get("organization") or "").strip())
        and _HASH.fullmatch(declared_hash) is not None
        and sha256(body.encode("utf-8")).hexdigest() == declared_hash
    )


def _tokens(value: str) -> list[str]:
    lowered = value.lower().replace("đ", "d")
    folded = "".join(char for char in unicodedata.normalize("NFD", lowered)
                     if unicodedata.category(char) != "Mn")
    return re.findall(r"[a-z0-9_]{2,}", folded)


def _value(value: object) -> str:
    return str(getattr(value, "value", value or "UNKNOWN"))


def _present(value: object) -> bool:
    if value == "PRESENT":
        return True
    if isinstance(value, list):
        return any(_present(item) for item in value)
    return isinstance(value, dict) and value.get("presence") == "PRESENT"


def _vietnam_priority(document: VerifiedDocument) -> int:
    """Prefer already-verified Vietnamese sources without weakening relevance/allowlist gates."""

    return int(document.domain == "vn" or document.domain.endswith(".vn"))


def _is_who(document: VerifiedDocument) -> bool:
    organization = f"{document.organization} {document.publisher}".casefold()
    return document.domain == "who.int" or document.domain.endswith(".who.int") or (
        "world health organization" in organization
    )


def _canonical_stage(stage: str) -> str:
    return {
        "POSTPARTUM": "POSTPARTUM_MOTHER",
        "INFANT": "INFANT_0_12M",
        "TODDLER": "TODDLER_12_24M",
    }.get(stage, stage)
