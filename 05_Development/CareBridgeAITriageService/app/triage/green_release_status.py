"""Explicit GREEN release assessment; it never changes a turn outcome."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from enum import Enum


class GreenReleaseStatus(str, Enum):
    DISABLED = "DISABLED"
    INTERNAL_TEST_ONLY = "INTERNAL_TEST_ONLY"
    BLOCKED_BY_SOURCE_COVERAGE = "BLOCKED_BY_SOURCE_COVERAGE"
    BLOCKED_BY_RULE_COVERAGE = "BLOCKED_BY_RULE_COVERAGE"
    BLOCKED_BY_DATASET = "BLOCKED_BY_DATASET"
    BLOCKED_BY_EVALUATION = "BLOCKED_BY_EVALUATION"
    ENABLED = "ENABLED"


@dataclass(frozen=True)
class GreenReleaseEvidence:
    runtime_green_requested: bool = False
    verified_source_count: int = 0
    source_coverage_complete: bool = False
    rule_coverage_complete: bool = False
    dataset_complete: bool = False
    evaluation_thresholds_met: bool = False
    full_e2e_passed: bool = False
    deletion_enforced: bool = False
    readiness_ready: bool = False
    ruleset_hash_match: bool = False
    #: The CareBridge development team's own release decision. This project has no clinician, so
    #: this is the only sign-off that can ever exist and the only one that gates a release.
    internal_dev_release_review: str | None = None
    #: Reported, never gated. CareBridge is an academic community project with no clinical
    #: reviewer, so gating on these would be an unsatisfiable condition dressed up as diligence —
    #: it would hide the real blockers behind one that can never be cleared. They are published
    #: so no reader can mistake this for a validated medical device.
    clinical_validation_status: str = "NOT_CLINICALLY_VALIDATED"
    external_clinical_sign_off: str = "NONE"


def assess_green_release(evidence: GreenReleaseEvidence) -> dict[str, object]:
    blockers: list[str] = []
    if evidence.verified_source_count <= 0 or not evidence.source_coverage_complete:
        blockers.append("SOURCE_COVERAGE_INCOMPLETE")
    if not evidence.rule_coverage_complete:
        blockers.append("RULE_COVERAGE_INCOMPLETE")
    if not evidence.dataset_complete:
        blockers.append("DATASET_INCOMPLETE")
    if not evidence.evaluation_thresholds_met:
        blockers.append("EVALUATION_THRESHOLDS_NOT_MET")
    if not evidence.full_e2e_passed:
        blockers.append("FULL_E2E_NOT_PASSED")
    if not evidence.deletion_enforced:
        blockers.append("RETENTION_DELETION_NOT_ENFORCED")
    if not evidence.readiness_ready:
        blockers.append("READINESS_NOT_READY")
    if not evidence.ruleset_hash_match:
        blockers.append("RULESET_HASH_MISMATCH")
    if not evidence.internal_dev_release_review:
        blockers.append("INTERNAL_DEV_RELEASE_REVIEW_MISSING")

    if "SOURCE_COVERAGE_INCOMPLETE" in blockers:
        eligibility = GreenReleaseStatus.BLOCKED_BY_SOURCE_COVERAGE
    elif "RULE_COVERAGE_INCOMPLETE" in blockers:
        eligibility = GreenReleaseStatus.BLOCKED_BY_RULE_COVERAGE
    elif "DATASET_INCOMPLETE" in blockers:
        eligibility = GreenReleaseStatus.BLOCKED_BY_DATASET
    elif any(item in blockers for item in (
        "EVALUATION_THRESHOLDS_NOT_MET", "FULL_E2E_NOT_PASSED",
        "RETENTION_DELETION_NOT_ENFORCED", "READINESS_NOT_READY", "RULESET_HASH_MISMATCH",
    )):
        eligibility = GreenReleaseStatus.BLOCKED_BY_EVALUATION
    elif blockers:
        eligibility = GreenReleaseStatus.INTERNAL_TEST_ONLY
    else:
        eligibility = GreenReleaseStatus.ENABLED

    # This is a report, not an enable switch. Runtime remains disabled unless a separately
    # requested, fully eligible, explicit release exists; the graph still has a hard GREEN deny.
    runtime = (GreenReleaseStatus.ENABLED if evidence.runtime_green_requested
               and eligibility is GreenReleaseStatus.ENABLED else GreenReleaseStatus.DISABLED)
    return {
        "runtimeStatus": runtime.value,
        "eligibilityStatus": eligibility.value,
        "blockers": blockers,
        "evidence": asdict(evidence),
        "automaticEnablement": False,
        # Three separate questions that are routinely conflated. A build can be technically
        # sound, still not fit to publish, and never clinically validated — all at once, and
        # that combination is a legitimate, honest state for this project.
        "technicalStatus": "READY" if evidence.readiness_ready and evidence.ruleset_hash_match
                           else "NOT_READY",
        "publicReleaseStatus": "READY" if eligibility is GreenReleaseStatus.ENABLED else "BLOCKED",
        "clinicalValidationStatus": evidence.clinical_validation_status,
        "externalClinicalSignOff": evidence.external_clinical_sign_off,
        "releaseReviewModel": "INTERNAL_DEV_RELEASE_REVIEW",
    }
