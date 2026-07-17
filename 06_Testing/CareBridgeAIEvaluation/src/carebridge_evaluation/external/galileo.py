"""Optional Galileo auxiliary experiment adapter.

The adapter follows the official generated-output experiment API and sends only case IDs,
sanitized generated output, and optional ground truth. It never sends raw intake text or PII.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from enum import StrEnum
from typing import Any


class GalileoStatus(StrEnum):
    COMPLETED = "COMPLETED"
    DISABLED = "DISABLED"
    SKIPPED_NO_EXTERNAL_KEY = "SKIPPED_NO_EXTERNAL_KEY"
    INFRASTRUCTURE_SKIPPED = "INFRASTRUCTURE_SKIPPED"


@dataclass(frozen=True)
class GalileoResult:
    status: GalileoStatus
    reason: str
    auxiliary_metrics: dict[str, Any]


class GalileoAdapter:
    def __init__(self, *, enabled: bool, api_key: str | None = None, project: str | None = None) -> None:
        self.enabled = enabled
        self.api_key = api_key or (os.getenv("GALILEO_API_KEY") or "").strip() or None
        self.project = project or (os.getenv("GALILEO_PROJECT") or "").strip() or None

    def run(self, records: list[dict[str, Any]]) -> GalileoResult:
        if not self.enabled:
            return GalileoResult(GalileoStatus.DISABLED, "GALILEO_ENABLED=false", {})
        if not self.api_key:
            return GalileoResult(GalileoStatus.SKIPPED_NO_EXTERNAL_KEY, "GALILEO_API_KEY is not configured", {})
        if not self.project:
            return GalileoResult(GalileoStatus.INFRASTRUCTURE_SKIPPED, "GALILEO_PROJECT is not configured", {})
        try:
            from galileo import GalileoMetrics
            from galileo.experiments import run_experiment
        except ImportError:
            return GalileoResult(GalileoStatus.INFRASTRUCTURE_SKIPPED, "The optional galileo SDK is not installed", {})

        sanitized = [
            {
                "input": str(record["caseId"]),
                "generated_output": str(record.get("generatedOutput") or ""),
                "ground_truth": str(record.get("groundTruth") or ""),
                "metadata": {"category": str(record.get("category") or "")},
            }
            for record in records
        ]
        result = run_experiment(
            "carebridge-ai-evaluation-auxiliary",
            dataset=sanitized,
            metrics=[GalileoMetrics.correctness],
            project=self.project,
            experiment_tags={"authority": "AUXILIARY_QUALITY_SCORE"},
        )
        return GalileoResult(
            GalileoStatus.COMPLETED,
            "Galileo auxiliary experiment completed; it does not affect deterministic pass/fail",
            {"providerResult": str(result), "authority": "AUXILIARY_QUALITY_SCORE"},
        )

