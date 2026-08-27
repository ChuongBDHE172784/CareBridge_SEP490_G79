"""Deterministic JSON, CSV, and simple HTML report generation."""

from __future__ import annotations

import csv
import html
from pathlib import Path
from typing import Any

from ..models import CaseResult, EvaluationRun

REPORT_CSV_COLUMNS = (
    "runId", "caseId", "category", "stage", "journeyPhase", "executionMode",
    "executionStatus", "reviewStatus", "expectedRisk", "actualRisk", "passed",
    "failureReasons", "latencyMs", "fallbackUsed", "assistantProvider", "disclaimerPassed",
    "diagnosisViolation", "prescriptionViolation", "dosageViolation", "promptInjectionPassed",
    "citationApprovedPassed", "citationDeepLinkPassed",
)


def write_reports(run: EvaluationRun, output_dir: Path) -> dict[str, Path]:
    output_dir.mkdir(parents=True, exist_ok=True)
    paths = {
        "json": output_dir / "report.json",
        "csv": output_dir / "report.csv",
        "html": output_dir / "report.html",
    }
    paths["json"].write_text(run.model_dump_json(indent=2) + "\n", encoding="utf-8")
    _write_csv(run, paths["csv"])
    paths["html"].write_text(_render_html(run), encoding="utf-8")
    return paths


def _write_csv(run: EvaluationRun, path: Path) -> None:
    with path.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=REPORT_CSV_COLUMNS, extrasaction="ignore")
        writer.writeheader()
        for result in run.results:
            writer.writerow(_csv_row(result))


def _csv_row(result: CaseResult) -> dict[str, Any]:
    payload = result.model_dump(mode="json")
    payload["category"] = result.category.value
    payload["stage"] = result.stage.value if result.stage else ""
    payload["journeyPhase"] = result.journeyPhase.value if result.journeyPhase else ""
    payload["executionMode"] = result.executionMode.value
    payload["executionStatus"] = result.executionStatus.value
    payload["reviewStatus"] = result.reviewStatus.value
    payload["failureReasons"] = " | ".join(result.failureReasons)
    return payload


def _render_html(run: EvaluationRun) -> str:
    summary_rows = "\n".join(
        f"<tr><td>{html.escape(name)}</td><td>{value}</td></tr>"
        for name, value in run.summary.model_dump().items()
    )
    metric_rows = "\n".join(
        "<tr>"
        f"<td>{html.escape(metric.metricId)}</td>"
        f"<td>{html.escape(metric.displayName)}</td>"
        f"<td>{_display(metric.value)}</td>"
        f"<td>{_display(metric.target)}</td>"
        f"<td>{html.escape(metric.status.value)}</td>"
        f"<td>{html.escape(metric.reason or '')}</td>"
        "</tr>"
        for metric in run.metrics
    )
    case_rows = "\n".join(
        "<tr>"
        f"<td>{html.escape(result.caseId)}</td>"
        f"<td>{html.escape(result.category.value)}</td>"
        f"<td>{html.escape(result.stage.value if result.stage else '')}</td>"
        f"<td>{html.escape(result.executionMode.value)}</td>"
        f"<td>{html.escape(result.executionStatus.value)}</td>"
        f"<td>{html.escape(result.expectedRisk or '')}</td>"
        f"<td>{html.escape(result.actualRisk or '')}</td>"
        f"<td>{html.escape(' | '.join(result.failureReasons))}</td>"
        "</tr>"
        for result in run.results
    )
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>CareBridge AI Evaluation {html.escape(run.runId)}</title>
  <style>
    body {{ font: 14px/1.45 system-ui, sans-serif; margin: 24px; color: #202124; }}
    table {{ border-collapse: collapse; width: 100%; margin-bottom: 28px; }}
    th, td {{ border: 1px solid #dfe1e5; padding: 7px; text-align: left; vertical-align: top; }}
    th {{ background: #f1f3f4; }}
    code {{ background: #f1f3f4; padding: 2px 4px; }}
  </style>
</head>
<body>
  <h1>CareBridge AI Evaluation</h1>
  <p>Run <code>{html.escape(run.runId)}</code> generated {html.escape(run.generatedAt.isoformat())}.</p>
  <p>Clinical validation: <strong>{html.escape(run.clinicalValidation)}</strong>. Migration status:
  <strong>{html.escape(run.migrationStatus)}</strong>.</p>
  <h2>Run summary</h2>
  <table><thead><tr><th>Count</th><th>Value</th></tr></thead><tbody>{summary_rows}</tbody></table>
  <h2>Metrics ({len(run.metrics)})</h2>
  <table><thead><tr><th>ID</th><th>Name</th><th>Value</th><th>Target</th><th>Status</th><th>Reason</th></tr></thead>
  <tbody>{metric_rows}</tbody></table>
  <h2>Cases ({len(run.results)})</h2>
  <table><thead><tr><th>Case</th><th>Category</th><th>Stage</th><th>Mode</th><th>Status</th><th>Expected</th><th>Actual</th><th>Reasons</th></tr></thead>
  <tbody>{case_rows}</tbody></table>
</body>
</html>
"""


def _display(value: Any) -> str:
    if value is None:
        return "N/A"
    if isinstance(value, float):
        return f"{value:.4f}"
    return html.escape(str(value))
