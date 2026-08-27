import csv
import json

from carebridge_evaluation.catalog import load_official_catalog
from carebridge_evaluation.metrics import calculate_metrics, calculate_run_summary
from carebridge_evaluation.models import (
    BenchmarkCategory,
    CaseResult,
    EvaluationRun,
    ExecutionMode,
    ExecutionStatus,
    ReviewStatus,
    TriageStage,
)
from carebridge_evaluation.reports import REPORT_CSV_COLUMNS, write_reports


def test_writes_all_mandatory_report_formats_with_case_rows(tmp_path):
    catalog = load_official_catalog()
    case = next(item for item in catalog.cases if item.id == "PEDIATRIC_RED_SEIZURE")
    run = EvaluationRun(results=[CaseResult(
        runId="run-1",
        caseId=case.id,
        category=BenchmarkCategory.PEDIATRIC_RED,
        stage=TriageStage.INFANT,
        executionMode=ExecutionMode.LOCAL_DETERMINISTIC,
        executionStatus=ExecutionStatus.PASSED,
        reviewStatus=ReviewStatus.CONFIRMED_REVIEWED,
        expectedRisk="RED",
        actualRisk="RED",
        passed=True,
        disclaimerPassed=True,
    )])
    run.metrics = calculate_metrics(catalog.cases, run.results)
    run.summary = calculate_run_summary(run.results)

    paths = write_reports(run, tmp_path)

    assert {"json", "csv", "html"} == set(paths)
    payload = json.loads(paths["json"].read_text(encoding="utf-8"))
    assert payload["runId"] == run.runId
    assert payload["summary"]["executedCases"] == 1
    with paths["csv"].open(encoding="utf-8-sig", newline="") as stream:
        rows = list(csv.DictReader(stream))
    assert rows[0]["caseId"] == case.id
    assert tuple(rows[0]) == REPORT_CSV_COLUMNS
    report_html = paths["html"].read_text(encoding="utf-8")
    assert "CareBridge AI Evaluation" in report_html
    assert "pendingMedicalReviewCases" in report_html
