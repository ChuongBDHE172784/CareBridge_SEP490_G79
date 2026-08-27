"""Sync the canonical AI Triage V2 rule registry into the Java and Python runtimes.

The registry under 05_Development/Contracts/triage/ is the single source of truth.
Java and Python each need a local copy because their Docker build contexts are
separate directories, so a shared path is not reachable at runtime.

This script copies each canonical file and writes a sidecar ``.sha256`` digest next
to every copy. The parity tests on both sides recompute the digest of their local
copy and compare it against that sidecar, so a hand-edited copy fails immediately.

Usage:
    python 05_Development/DevTools/sync_triage_rule_registry.py            # write copies
    python 05_Development/DevTools/sync_triage_rule_registry.py --check    # verify only
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
CANONICAL_DIR = REPO_ROOT / "05_Development" / "Contracts" / "triage"

# canonical filename -> list of destination directories
TARGETS: dict[str, list[Path]] = {
    "triage_rules_v2.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "triage_rule_condition.schema.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "context_parity_vectors_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "test" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "tests" / "data",
    ],
    "triage_rule_parity_vectors_v2.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "test" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "tests" / "data",
    ],
    "required_rule_manifest.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "internal_rule_review_manifest.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "source_verification_manifest.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "context_contract_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "target_entity_indicators_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "intent_indicators_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "question_catalog_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "canonical_answer_mapping_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "oos_complaint_taxonomy_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "matrix_snapshot_v0.1.0.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "dataset_requirements_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
    "draft_safety_disposition_matrix_v1.json": [
        REPO_ROOT / "05_Development" / "CareBridgeAPI" / "src" / "main" / "resources" / "triage",
        REPO_ROOT / "05_Development" / "CareBridgeAITriageService" / "data",
    ],
}

INTEGRITY_MANIFEST = CANONICAL_DIR / "artifact_integrity_manifest.json"


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def update_integrity_manifest(check_only: bool, problems: list[str]) -> None:
    """Regenerate the artifact integrity manifest.

    This manifest proves only that the listed files match their recorded checksums. It says
    nothing about whether the content is medically correct and it is NOT an approval record —
    provenance lives in source_verification_manifest.json and review in
    internal_rule_review_manifest.json.
    """

    artifacts = []
    for path in sorted(CANONICAL_DIR.glob("*.json")):
        if path.name == INTEGRITY_MANIFEST.name:
            continue
        artifacts.append({
            "artifactId": path.stem,
            "path": str(path.relative_to(REPO_ROOT)).replace("\\", "/"),
            "sha256": sha256_of(path),
        })

    manifest = {
        "$comment": ("Integrity only. Proves each artifact matches its checksum. Does NOT prove "
                     "clinical correctness and is NOT an approval record."),
        "manifestType": "ARTIFACT_INTEGRITY",
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "generatorVersion": "sync_triage_rule_registry.py/3.0",
        "artifacts": artifacts,
    }

    if check_only:
        if not INTEGRITY_MANIFEST.is_file():
            problems.append(f"missing integrity manifest: {INTEGRITY_MANIFEST}")
            return
        recorded = json.loads(INTEGRITY_MANIFEST.read_text(encoding="utf-8"))
        recorded_map = {item["artifactId"]: item["sha256"] for item in recorded.get("artifacts", [])}
        current_map = {item["artifactId"]: item["sha256"] for item in artifacts}
        if recorded_map != current_map:
            problems.append("artifact integrity manifest is stale")
        return

    INTEGRITY_MANIFEST.write_text(
        json.dumps(manifest, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(f"updated {INTEGRITY_MANIFEST.relative_to(REPO_ROOT)}")


def derive_source_verification(check_only: bool, problems: list[str]) -> None:
    """Derive each rule's sourceVerificationStatus from the source manifest.

    The registry previously carried a hand-set ``SOURCE_VERIFIED`` on every rule while the
    manifest said the sources were still PENDING. Two artifacts disagreeing about whether the
    evidence was checked is worse than either answer alone, so the value is now computed and
    the registry copy is never authoritative.
    """

    manifest_path = CANONICAL_DIR / "source_verification_manifest.json"
    registry_path = CANONICAL_DIR / "triage_rules_v2.json"
    if not manifest_path.is_file() or not registry_path.is_file():
        problems.append("cannot derive source verification: manifest or registry missing")
        return

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    status_by_source = {s["sourceId"]: s.get("verificationStatus", "PENDING")
                        for s in manifest.get("sources", [])}
    registry = json.loads(registry_path.read_text(encoding="utf-8"))

    changed = False
    for rule in registry.get("rules", []):
        statuses = [status_by_source.get(sid, "PENDING") for sid in rule.get("sourceIds", [])]
        if not statuses:
            # A rule with no cited source is a system behaviour rule, not an evidence claim.
            derived = "NOT_APPLICABLE"
        elif "BROKEN" in statuses:
            derived = "BROKEN"
        elif "SOURCE_CHANGED" in statuses:
            derived = "SOURCE_CHANGED"
        elif all(status == "SOURCE_VERIFIED" for status in statuses):
            derived = "SOURCE_VERIFIED"
        else:
            derived = "PENDING"
        if rule.get("sourceVerificationStatus") != derived:
            if check_only:
                problems.append(
                    f"rule {rule.get('ruleId')} sourceVerificationStatus is "
                    f"{rule.get('sourceVerificationStatus')!r}, derived value is {derived!r}")
            else:
                rule["sourceVerificationStatus"] = derived
                changed = True

    if changed and not check_only:
        registry_path.write_text(
            json.dumps(registry, indent=2, ensure_ascii=False) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        print("derived rule sourceVerificationStatus from source_verification_manifest.json")


def sync(check_only: bool) -> int:
    problems: list[str] = []
    # Derive before copying so the runtime copies carry the computed value.
    derive_source_verification(check_only, problems)
    for filename, destinations in TARGETS.items():
        canonical = CANONICAL_DIR / filename
        if not canonical.is_file():
            problems.append(f"missing canonical file: {canonical}")
            continue
        digest = sha256_of(canonical)
        for destination in destinations:
            copy = destination / filename
            sidecar = destination / f"{filename}.sha256"
            if check_only:
                if not copy.is_file():
                    problems.append(f"missing copy: {copy}")
                elif sha256_of(copy) != digest:
                    problems.append(f"copy drifted from canonical: {copy}")
                elif not sidecar.is_file() or sidecar.read_text(encoding="utf-8").strip() != digest:
                    problems.append(f"stale or missing digest sidecar: {sidecar}")
                continue
            destination.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(canonical, copy)
            sidecar.write_text(digest + "\n", encoding="utf-8", newline="\n")
            print(f"synced {copy.relative_to(REPO_ROOT)}")

    update_integrity_manifest(check_only, problems)

    if problems:
        for problem in problems:
            print(f"ERROR: {problem}", file=sys.stderr)
        return 1
    print("triage rule registry: OK" if check_only else "triage rule registry: synced")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="verify copies without writing")
    args = parser.parse_args()
    return sync(args.check)


if __name__ == "__main__":
    raise SystemExit(main())
