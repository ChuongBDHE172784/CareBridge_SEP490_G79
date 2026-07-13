from __future__ import annotations

import re
import threading
from datetime import datetime, timedelta, timezone
from pathlib import Path

import frontmatter

from app.config import EVIDENCE_CACHE_TTL_DAYS, MEDICAL_SOURCES_PENDING_DIR
from app.schemas import SourceDocument


_CACHE_WRITE_LOCK = threading.Lock()


def cache_pending_source(source: SourceDocument, cache_dir: Path | None = None) -> Path:
    with _CACHE_WRITE_LOCK:
        return _cache_pending_source_locked(source, cache_dir)


def _cache_pending_source_locked(source: SourceDocument, cache_dir: Path | None = None) -> Path:
    target_dir = cache_dir or MEDICAL_SOURCES_PENDING_DIR
    target_dir.mkdir(parents=True, exist_ok=True)
    filename = f"{_safe_slug(source.domain)}_{_safe_slug(source.id)}.md"
    path = target_dir / filename
    if path.exists():
        post = frontmatter.load(path)
        retrieved = post.metadata.get("retrievedAt")
        if retrieved:
            try:
                timestamp = datetime.fromisoformat(str(retrieved).replace("Z", "+00:00"))
                if timestamp.tzinfo is None:
                    timestamp = timestamp.replace(tzinfo=timezone.utc)
                if datetime.now(timezone.utc) - timestamp < timedelta(days=EVIDENCE_CACHE_TTL_DAYS):
                    return path
            except (TypeError, ValueError):
                pass

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
        sourceVersion=source.sourceVersion,
        retrievedAt=source.retrievedAt,
        retrievedBy="realtime_official_search",
        matchedSymptoms=source.symptoms,
        adminReviewed=False,
        etag=source.etag,
        lastModified=source.lastModified,
        ttlDays=EVIDENCE_CACHE_TTL_DAYS,
    )
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(frontmatter.dumps(post), encoding="utf-8")
    temporary.replace(path)
    return path


def cache_pending_sources(sources: list[SourceDocument]) -> list[Path]:
    return [cache_pending_source(source) for source in sources]


def _safe_slug(value: str) -> str:
    slug = re.sub(r"[^a-zA-Z0-9_-]+", "_", value.strip().lower())
    return slug.strip("_") or "source"
