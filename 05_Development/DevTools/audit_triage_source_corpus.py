"""Audit triage reading sources without promoting or rewriting their verification status."""

from __future__ import annotations

import argparse
import json
import re
from hashlib import sha256
from pathlib import Path
from urllib.parse import urlparse

import frontmatter

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_CORPUS = (
    REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data" / "medical_sources"
)
RULE_REGISTRY = REPO_ROOT / "05_Development" / "Contracts" / "triage" / "triage_rules_v2.json"
HASH = re.compile(r"^[0-9a-f]{64}$")
TRUST_STATUSES = {
    "IDENTIFIED_NOT_VALIDATED",
    "DOMAIN_AND_DOCUMENT_VERIFIED",
    "REJECTED",
}
CANONICAL_STAGES = {
    "PRECONCEPTION",
    "POSSIBLE_PREGNANCY",
    "PREGNANCY",
    "POSTPARTUM_MOTHER",
    "INFANT_0_12M",
    "TODDLER_12_24M",
}
STAGE_ALIASES = {
    "POSTPARTUM": "POSTPARTUM_MOTHER",
    "INFANT": "INFANT_0_12M",
    "TODDLER": "TODDLER_12_24M",
}


def audit_corpus(corpus_dir: Path = DEFAULT_CORPUS) -> dict[str, object]:
    rule_stages = _rule_stages()
    documents: list[dict[str, object]] = []
    for path in sorted(corpus_dir.glob("*.md")):
        reasons: list[str] = []
        try:
            post = frontmatter.load(path)
            metadata = post.metadata
            body = post.content.strip()
        except Exception:
            documents.append({"file": path.name, "passed": False,
                              "reasons": ["MALFORMED_DOCUMENT"]})
            continue
        trust = metadata.get("publisherTrustStatus")
        if trust not in TRUST_STATUSES:
            reasons.append("INVALID_PUBLISHER_TRUST_STATUS")
        if trust != "DOMAIN_AND_DOCUMENT_VERIFIED":
            reasons.append("PUBLISHER_IDENTITY_IS_NOT_DOCUMENT_VERIFICATION")
        url = str(metadata.get("url") or "")
        domain = str(metadata.get("domain") or "").lower().removeprefix("www.")
        parsed = urlparse(url)
        host = (parsed.hostname or "").lower().removeprefix("www.")
        if parsed.scheme != "https" or not parsed.path.strip("/") or not domain or not (
            host == domain or host.endswith(f".{domain}")
        ):
            reasons.append("INVALID_HTTPS_DOMAIN_PROVENANCE")
        if not str(metadata.get("sectionOrPage") or "").strip():
            reasons.append("MISSING_SECTION_OR_PAGE")
        declared_hash = str(metadata.get("contentHash") or "").lower()
        if HASH.fullmatch(declared_hash) is None:
            reasons.append("MISSING_OR_INVALID_CONTENT_HASH")
        elif sha256(body.encode("utf-8")).hexdigest() != declared_hash:
            reasons.append("CONTENT_HASH_MISMATCH")
        targets = metadata.get("targetEntities")
        stages = metadata.get("applicableStages")
        rule_ids = metadata.get("ruleIds")
        if not targets:
            reasons.append("MISSING_TARGET_MAPPING")
        elif not isinstance(targets, list) or any(
            target not in {"MOTHER", "BABY"} for target in targets
        ):
            reasons.append("INVALID_TARGET_MAPPING")
        if not stages:
            reasons.append("MISSING_STAGE_MAPPING")
            canonical_stages: set[str] = set()
        elif not isinstance(stages, list):
            reasons.append("INVALID_STAGE_MAPPING")
            canonical_stages = set()
        else:
            canonical_stages = {STAGE_ALIASES.get(str(stage), str(stage)) for stage in stages}
            if not canonical_stages <= CANONICAL_STAGES:
                reasons.append("INVALID_STAGE_MAPPING")
        if isinstance(targets, list) and canonical_stages:
            expected_targets = {
                "BABY" if stage in {"INFANT_0_12M", "TODDLER_12_24M"} else "MOTHER"
                for stage in canonical_stages
            }
            if not set(targets) <= expected_targets:
                reasons.append("TARGET_STAGE_MAPPING_MISMATCH")
        if not rule_ids:
            reasons.append("MISSING_RULE_MAPPING")
        elif not isinstance(rule_ids, list) or any(rule_id not in rule_stages for rule_id in rule_ids):
            reasons.append("INVALID_RULE_MAPPING")
        elif canonical_stages and any(
            not canonical_stages & rule_stages[rule_id] for rule_id in rule_ids
        ):
            reasons.append("RULE_STAGE_MAPPING_MISMATCH")
        if metadata.get("sourceStatus") != "SOURCE_VERIFIED":
            reasons.append("SOURCE_STATUS_NOT_VERIFIED")
        documents.append({
            "file": path.name,
            "sourceId": metadata.get("id") or path.stem,
            "publisherTrustStatus": trust,
            "passed": not reasons,
            "reasons": reasons,
        })
    passed = sum(1 for document in documents if document["passed"])
    return {
        "corpus": str(corpus_dir),
        "summary": {"documents": len(documents), "passed": passed,
                    "failed": len(documents) - passed},
        "documents": documents,
        "note": "Publisher identity or reputation is not clinical validation.",
    }


def _rule_stages() -> dict[str, set[str]]:
    payload = json.loads(RULE_REGISTRY.read_text(encoding="utf-8"))
    result: dict[str, set[str]] = {}
    for rule in payload.get("rules", []):
        result[str(rule.get("ruleId"))] = {
            STAGE_ALIASES.get(str(stage), str(stage)) for stage in rule.get("stages", [])
        }
    for policy in payload.get("safetyPolicies", []):
        result[str(policy.get("policyId"))] = {
            STAGE_ALIASES.get(str(stage), str(stage)) for stage in policy.get("stages", [])
        }
    return result


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus", type=Path, default=DEFAULT_CORPUS)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    report = audit_corpus(args.corpus)
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered, encoding="utf-8", newline="\n")
    print(rendered, end="")
    return 0 if report["summary"]["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
