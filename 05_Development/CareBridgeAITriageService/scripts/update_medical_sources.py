from __future__ import annotations

import json
import re
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse
from urllib.request import Request, urlopen

import frontmatter

ROOT_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT_DIR / "data"
SOURCES_FILE = DATA_DIR / "official_sources.json"
MEDICAL_SOURCES_DIR = DATA_DIR / "medical_sources"

WHITELIST = {
    "who.int",
    "moh.gov.vn",
    "mch.moh.gov.vn",
    "cdc.gov",
    "unicef.org",
    "benhviennhitrunguong.gov.vn",
    "nhidong.org.vn",
    "bvndtp.org.vn",
}


def main() -> int:
    sources = json.loads(SOURCES_FILE.read_text(encoding="utf-8"))
    MEDICAL_SOURCES_DIR.mkdir(parents=True, exist_ok=True)
    updated_at = datetime.now(timezone.utc).isoformat()
    for source in sources:
        url = source["url"]
        domain = source.get("domain", "")
        if not _is_allowed(url, domain):
            print(f"SKIP non-whitelisted source: {url}")
            continue

        source_id = source.get("id") or _make_source_id(source)
        target = MEDICAL_SOURCES_DIR / f"{source_id.lower()}.md"
        if target.exists() and _admin_reviewed(target):
            print(f"SKIP admin-reviewed source: {target.name}")
            continue

        body = _fetch_preview(url)
        post = frontmatter.Post(
            body,
            id=source_id,
            title=source["title"],
            organization=source["organization"],
            url=url,
            domain=domain,
            topic=source.get("topic", "general_child_health"),
            ageRange=source.get("ageRange", "child"),
            riskLevels=source.get("riskLevels", []),
            symptoms=source.get("symptoms", []),
            lastReviewed=updated_at[:10],
            sourceType=source.get("sourceType", "official_guideline"),
            adminReviewed=False,
            updatedAt=updated_at,
        )
        target.write_text(frontmatter.dumps(post), encoding="utf-8")
        print(f"SYNC {target.name}")
    print(f"Medical source sync completed at {updated_at}")
    return 0


def _is_allowed(url: str, domain: str) -> bool:
    parsed = urlparse(url)
    host = (parsed.hostname or "").lower().removeprefix("www.")
    path = parsed.path.rstrip("/").lower()
    domain = domain.lower().strip()
    if parsed.scheme != "https" or path in {"", "/vi", "/en"}:
        return False
    if domain and domain not in WHITELIST:
        return False
    return any(host == allowed or host.endswith(f".{allowed}") for allowed in WHITELIST)


def _admin_reviewed(path: Path) -> bool:
    post = frontmatter.load(path)
    return bool(post.metadata.get("adminReviewed"))


def _fetch_preview(url: str) -> str:
    try:
        request = Request(url, headers={"User-Agent": "CareBridgeAITriageSourceSync/1.0"})
        with urlopen(request, timeout=10) as response:
            text = response.read(4096).decode("utf-8", errors="ignore")
    except Exception as exc:
        return (
            "Imported official source placeholder. Content fetch failed during sync; "
            f"admin review required before use. Error: {exc.__class__.__name__}"
        )
    compact = " ".join(text.split())
    return (
        compact[:1200]
        if compact
        else "Imported official source placeholder. Admin review required before use."
    )


def _make_source_id(source: dict) -> str:
    raw = f"{source.get('organization', '')}_{source.get('title', '')}"
    slug = re.sub(r"[^A-Za-z0-9]+", "_", raw).strip("_").upper()
    return slug or "OFFICIAL_SOURCE"


if __name__ == "__main__":
    sys.exit(main())
