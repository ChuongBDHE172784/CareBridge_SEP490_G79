from carebridge_evaluation.catalog import load_official_catalog
from carebridge_evaluation.metrics import METRIC_IDS, calculate_metrics, calculate_run_summary
from carebridge_evaluation.models import (
    BenchmarkCategory,
    CaseResult,
    ExecutionMode,
    ExecutionStatus,
    MetricStatus,
    ReviewStatus,
    TriageStage,
)

EXPECTED_IDS = (
    "totalCases", "passedCases", "failedCases", "skippedCases", "redRecall",
    "unsafeGreenRate", "needMoreInfoAccuracy", "emergencyOverridePassRate",
    "disclaimerCompliance", "professionalConsultationReminderCompliance",
    "diagnosisViolationRate", "prescriptionViolationRate", "dosageViolationRate",
    "promptInjectionPassRate", "approvedSourceCitationRate", "deepLinkCitationRate",
    "citationDomainMatchRate", "latencyP50Ms", "latencyP95Ms", "fallbackRate", "errorRate",
)


def result(case_id: str, *, review=ReviewStatus.PENDING_MEDICAL_REVIEW, expected="RED", actual="RED"):
    return CaseResult(
        runId="run-1",
        caseId=case_id,
        category=BenchmarkCategory.PEDIATRIC_RED,
        stage=TriageStage.INFANT,
        executionMode=ExecutionMode.LOCAL_DETERMINISTIC,
        executionStatus=ExecutionStatus.PASSED,
        reviewStatus=review,
        expectedRisk=expected,
        actualRisk=actual,
        passed=True,
        latencyMs=10,
        fallbackUsed=False,
        disclaimerPassed=True,
        diagnosisViolation=False,
        prescriptionViolation=False,
        dosageViolation=False,
        guaranteedOutcomeViolation=False,
        emergencyDelayViolation=False,
        response={"riskLevel": actual, "disclaimer": "fixed"},
    )


def test_registry_has_exactly_the_approved_21_stable_ids():
    assert METRIC_IDS == EXPECTED_IDS
    assert len(METRIC_IDS) == 21
    assert len(set(METRIC_IDS)) == 21


def test_pending_medical_review_is_excluded_from_red_clinical_denominator():
    catalog = load_official_catalog()
    pending_case = next(case for case in catalog.cases if case.id == "PEDIATRIC_RED_SEIZURE")
    metrics = {metric.metricId: metric for metric in calculate_metrics(catalog.cases, [result(pending_case.id)])}

    assert metrics["redRecall"].denominator == 0
    assert metrics["redRecall"].value is None
    assert metrics["redRecall"].status == MetricStatus.NOT_EVALUATED
    assert metrics["disclaimerCompliance"].denominator == 1


def test_technical_need_more_info_case_is_eligible_without_clinical_review():
    catalog = load_official_catalog()
    case = next(item for item in catalog.cases if item.id == "NMI_UNKNOWN_SYMPTOM")
    item = result(case.id, review=ReviewStatus.NOT_APPLICABLE, expected="NEED_MORE_INFO", actual="NEED_MORE_INFO")
    item.category = BenchmarkCategory.NEED_MORE_INFO
    metrics = {metric.metricId: metric for metric in calculate_metrics(catalog.cases, [item])}

    assert metrics["needMoreInfoAccuracy"].value == 1.0


def test_run_summary_is_not_part_of_metric_registry():
    pending = result("PEDIATRIC_RED_SEIZURE")
    skipped = result("RAG_APPROVED_STAGE_AND_CLAIM_MAPPING", review=ReviewStatus.NOT_APPLICABLE)
    skipped.executionStatus = ExecutionStatus.INFRASTRUCTURE_SKIPPED
    skipped.passed = None

    summary = calculate_run_summary([pending, skipped])

    assert summary.executedCases == 1
    assert summary.infrastructureSkippedCases == 1
    assert summary.pendingMedicalReviewCases == 1
    assert summary.notApplicableCases == 1
    assert "executedCases" not in METRIC_IDS


def test_zero_denominator_metrics_are_null_and_not_evaluated():
    metrics = calculate_metrics(load_official_catalog().cases, [])
    rate = next(metric for metric in metrics if metric.metricId == "redRecall")

    assert rate.denominator == 0
    assert rate.value is None
    assert rate.status == MetricStatus.NOT_EVALUATED
    assert rate.reason
