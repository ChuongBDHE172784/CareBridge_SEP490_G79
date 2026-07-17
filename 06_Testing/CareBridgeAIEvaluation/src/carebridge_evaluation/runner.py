"""Evaluation orchestration independent of production triage decisions."""

from __future__ import annotations

from .adapters import ApiAdapter, LocalAdapter
from .assertions import apply_registry_scan_assertions
from .catalog import load_official_catalog
from .config import EvaluationSettings
from .metrics import calculate_metrics, calculate_run_summary
from .models import EvaluationRun
from .registry_scan import scan_hardcoded_registry


def run_evaluation(mode: str, settings: EvaluationSettings | None = None) -> EvaluationRun:
    if mode not in {"local", "api", "all"}:
        raise ValueError("mode must be local, api, or all")
    settings = settings or EvaluationSettings.from_env()
    catalog = load_official_catalog()
    run = EvaluationRun()

    if mode in {"local", "all"}:
        local = LocalAdapter(settings.ai_service_path)
        run.results.extend(local.execute(case, run.runId) for case in catalog.cases)
    if mode in {"api", "all"}:
        api = ApiAdapter(settings)
        api.preflight()
        run.results.extend(api.execute(case, run.runId) for case in catalog.cases)
    registry_violations = scan_hardcoded_registry(settings.ai_service_path)
    run.auxiliaryMetrics["hardcodedRegistryScan"] = {
        "violations": registry_violations,
        "scannedPath": str(settings.ai_service_path / "app"),
    }
    apply_registry_scan_assertions(catalog.cases, run.results, registry_violations)
    run.summary = calculate_run_summary(run.results)
    run.metrics = calculate_metrics(catalog.cases, run.results)
    return run
