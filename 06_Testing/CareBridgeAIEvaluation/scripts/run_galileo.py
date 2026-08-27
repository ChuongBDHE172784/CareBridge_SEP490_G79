"""Optionally send minimized report outputs to Galileo as auxiliary metrics."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path

from carebridge_evaluation.external import GalileoAdapter


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    args = parser.parse_args()
    report = json.loads(args.report.read_text(encoding="utf-8"))
    records = [
        {
            "caseId": result["caseId"],
            "category": result["category"],
            "generatedOutput": result.get("actualRisk") or result.get("executionStatus"),
            "groundTruth": result.get("expectedRisk") or "",
        }
        for result in report.get("results", [])
    ]
    adapter = GalileoAdapter(enabled=os.getenv("GALILEO_ENABLED", "false").lower() == "true")
    result = adapter.run(records)
    print(json.dumps({
        "status": result.status.value,
        "reason": result.reason,
        "auxiliaryMetrics": result.auxiliary_metrics,
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
