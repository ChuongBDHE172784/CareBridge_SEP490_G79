from __future__ import annotations

import re
from pathlib import Path

import frontmatter

from app.config import MEDICAL_SOURCES_PENDING_DIR
from app.schemas import SourceDocument


def cache_pending_source(source: SourceDocument, cache_dir: Path | None = None) -> Path:
    target_dir = cache_dir or MEDICAL_SOURCES_PENDING_DIR
    target_dir.mkdir(parents=True, exist_ok=True)
    filename = f"{_safe_slug(source.domain)}_{_safe_slug(source.id)}.md"
    path = target_dir / filename
    if path.exists():
        return path

    post = frontmatter.Post(
        source.body,
        id=source.id,
        title=source.title,
        organization=source.organization,
        url=source.url,
        domain=source.domain,
        topic=source.topic,
        ageRange=source.ageRange,
        riskLevels=source.riskLevels,
        symptoms=source.symptoms,
        lastReviewed=source.lastReviewed,
        sourceType=source.sourceType,
        sourceStatus="PENDING_REVIEW",
        retrievedAt=source.retrievedAt,
        retrievedBy="realtime_official_search",
        matchedSymptoms=source.symptoms,
        adminReviewed=False,
    )
    path.write_text(frontmatter.dumps(post), encoding="utf-8")
    return path


def cache_pending_sources(sources: list[SourceDocument]) -> list[Path]:
    return [cache_pending_source(source) for source in sources]


def _safe_slug(value: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9_-]+", "_", value.strip().lower())
    return slug.strip("_") or "source"
