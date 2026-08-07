from __future__ import annotations

import json

from scripts.run_triage_v2_evaluation import (
    DEFAULT_CASES,
    REQUIRED_EVALUATION_COVERAGE,
    REQUIRED_METRIC_COVERAGE,
    evaluate,
)


def test_report_covers_every_required_evaluation_category_and_metric_honestly():
    report = evaluate(DEFAULT_CASES)

    assert report["failed"] == 0
    assert report["coverage"]["allRequiredCategoriesCovered"] is True
    assert report["coverage"]["allRequiredMetricFieldsReported"] is True
    assert set(report["coverage"]["requiredEvaluationCategories"]) == set(
        REQUIRED_EVALUATION_COVERAGE
    )
    assert set(report["coverage"]["requiredMetrics"]) == set(REQUIRED_METRIC_COVERAGE)

    metrics = report["metrics"]
    assert metrics["redFalseNegativeCount"] == 0
    assert metrics["redFalseNegativeRate"] == 0
    assert metrics["unsupportedGreenCount"] == 0
    assert metrics["wrongQuestionCount"] == 0
    assert metrics["citationPrecision"] is None
    assert metrics["brokenOrUnverifiedLinkExposureCount"] is None
    assert metrics["schemaFailureCount"] is None
    assert metrics["fallbackCount"] is None
    assert metrics["parityMismatchCount"] is None
    assert metrics["maxRoundRouteCount"] == 1
    red_results = [
        item for item in report["results"] if item["category"] in {"GLOBAL_RED", "STAGE_RED"}
    ]
    assert red_results
    assert {item["inputMode"] for item in red_results} == {"STRUCTURED_SIGNALS"}


def test_every_declared_component_evidence_reference_resolves_to_a_test_symbol():
    report = evaluate(DEFAULT_CASES)

    for evidence in report["coverage"]["requiredEvaluationCategories"].values():
        assert evidence["missingCaseIds"] == []
        assert evidence["missingTestEvidence"] == []


def test_empty_corpus_reports_missing_coverage_without_division_errors(tmp_path):
    path = tmp_path / "empty.json"
    path.write_text(json.dumps([]), encoding="utf-8")

    report = evaluate(path)

    assert report["total"] == 0
    assert report["metrics"]["redRecall"] is None
    assert report["metrics"]["targetAccuracy"] is None
    assert report["coverage"]["allRequiredCategoriesCovered"] is False
