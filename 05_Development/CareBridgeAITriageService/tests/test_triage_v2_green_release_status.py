from app.triage_v2.green_release_status import (
    GreenReleaseEvidence, GreenReleaseStatus, assess_green_release,
)
from app.triage_v2.renderer_audit import green_release_gate


def test_current_posture_is_disabled_and_blocked_by_source_coverage():
    report = assess_green_release(GreenReleaseEvidence(
        evaluation_thresholds_met=True, readiness_ready=True, ruleset_hash_match=True,
    ))

    assert report["runtimeStatus"] == GreenReleaseStatus.DISABLED
    assert report["eligibilityStatus"] == GreenReleaseStatus.BLOCKED_BY_SOURCE_COVERAGE
    assert report["automaticEnablement"] is False
    assert "INTERNAL_DEV_RELEASE_REVIEW_MISSING" in report["blockers"]


def test_clinical_status_is_reported_but_never_used_as_a_release_blocker():
    """No clinician exists on this project, so gating on one would be permanently unsatisfiable
    and would mask the blockers that can actually be cleared."""

    report = assess_green_release(GreenReleaseEvidence())

    assert report["clinicalValidationStatus"] == "NOT_CLINICALLY_VALIDATED"
    assert report["externalClinicalSignOff"] == "NONE"
    assert report["releaseReviewModel"] == "INTERNAL_DEV_RELEASE_REVIEW"
    assert "NOT_CLINICALLY_VALIDATED" not in report["blockers"]
    assert "EXTERNAL_CLINICAL_SIGN_OFF_NONE" not in report["blockers"]


def test_technical_release_and_clinical_status_are_reported_separately():
    """A build may be technically sound, not publishable, and never clinically validated at the
    same time; the report must be able to say exactly that."""

    report = assess_green_release(GreenReleaseEvidence(
        readiness_ready=True, ruleset_hash_match=True,
    ))

    assert report["technicalStatus"] == "READY"
    assert report["publicReleaseStatus"] == "BLOCKED"
    assert report["clinicalValidationStatus"] == "NOT_CLINICALLY_VALIDATED"


def test_even_complete_evidence_does_not_enable_without_runtime_request():
    report = assess_green_release(GreenReleaseEvidence(
        verified_source_count=1, source_coverage_complete=True, rule_coverage_complete=True,
        dataset_complete=True, evaluation_thresholds_met=True, full_e2e_passed=True,
        deletion_enforced=True, readiness_ready=True, ruleset_hash_match=True,
        internal_dev_release_review="RECORDED-INTERNAL-DEV-DECISION",
    ))

    assert report["eligibilityStatus"] == GreenReleaseStatus.ENABLED
    assert report["runtimeStatus"] == GreenReleaseStatus.DISABLED
    # Full eligibility must still not require a clinical sign-off that cannot exist.
    assert report["clinicalValidationStatus"] == "NOT_CLINICALLY_VALIDATED"


def test_graph_green_deny_remains_independent_of_release_assessment():
    update = green_release_gate({"triageOutcome": "GREEN"})
    assert update["triageOutcome"] == "NEEDS_MORE_INFO"
    assert update["reasonCodes"] == ["GREEN_RELEASE_GATE_DISABLED"]
