"""Produce the current, fail-closed GREEN release decision artifact."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


SERVICE_ROOT = Path(__file__).resolve().parents[1]
if str(SERVICE_ROOT) not in sys.path:
    sys.path.insert(0, str(SERVICE_ROOT))

from app.triage.evidence_retrieval import corpus_inventory
from app.triage.green_release_status import GreenReleaseEvidence, assess_green_release


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evaluation", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    evaluation = json.loads(args.evaluation.read_text(encoding="utf-8"))
    inventory = corpus_inventory()
    metrics = evaluation.get("metrics", {})
    evaluation_ok = (
        evaluation.get("failed") == 0
        and metrics.get("redFalseNegativeCount") == 0
        and metrics.get("unsupportedGreenCount") == 0
        and metrics.get("wrongQuestionCount") == 0
    )
    evidence = GreenReleaseEvidence(
        runtime_green_requested=False,
        verified_source_count=inventory["declaredSourceVerified"],
        source_coverage_complete=False,
        rule_coverage_complete=False,
        dataset_complete=False,
        evaluation_thresholds_met=evaluation_ok,
        full_e2e_passed=False,
        deletion_enforced=False,
        readiness_ready=True,
        ruleset_hash_match=True,
        internal_dev_release_review=None,
        clinical_validation_status="NOT_CLINICALLY_VALIDATED",
        external_clinical_sign_off="NONE",
    )
    report = assess_green_release(evidence)
    report["governance"] = {
        "projectType": "ACADEMIC_COMMUNITY_PROJECT",
        "intendedUse": "INFORMATIONAL_RISK_ORIENTATION",
        "internalReviewStatus": "DEV_REVIEWED",
        "clinicalValidationStatus": "NOT_CLINICALLY_VALIDATED",
        "externalClinicalSignOff": "NONE",
        "releaseReviewModel": "INTERNAL_DEV_RELEASE_REVIEW",
    }
    # `retentionUntil` is stamped on each session but nothing consumes it: there is no purge job
    # and no cascade across sessions, messages, state, signals, health memory, evidence links,
    # audit or consent. The project has no agreed retention contract, and one must not be
    # invented here — deleting real user data needs an approved policy, not a default.
    report["dataRetention"] = {
        "status": "DATA_RETENTION_POLICY_REQUIRED",
        "enforcementImplemented": False,
        "note": "retentionUntil is recorded but never acted on; public release stays blocked.",
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
