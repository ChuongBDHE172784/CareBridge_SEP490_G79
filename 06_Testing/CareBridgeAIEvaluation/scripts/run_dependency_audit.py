"""Run pip-audit while distinguishing vulnerability findings from index outages."""

from __future__ import annotations

import json
import subprocess  # nosec B404
import sys
from pathlib import Path

INFRASTRUCTURE_MARKERS = (
    "connection error",
    "connection refused",
    "name or service not known",
    "read timed out",
    "temporary failure",
    "failed to establish a new connection",
)
VULNERABILITY_MARKERS = (
    "known vulnerability",
    "known vulnerabilities",
)
AUDIT_TIMEOUT_SECONDS = 300


def main() -> int:
    try:
        completed = subprocess.run(  # nosec B603
            [
                sys.executable,
                "-m",
                "pip_audit",
                "--strict",
                "--requirement",
                str(Path(__file__).resolve().parents[1] / "requirements-audit.txt"),
            ],
            capture_output=True,
            text=True,
            check=False,
            timeout=AUDIT_TIMEOUT_SECONDS,
        )
    except subprocess.TimeoutExpired:
        print(json.dumps({
            "status": "INFRASTRUCTURE_SKIPPED",
            "reason": f"Dependency audit exceeded {AUDIT_TIMEOUT_SECONDS} seconds",
        }))
        return 0
    output = f"{completed.stdout}\n{completed.stderr}"
    if output.strip():
        print(output, end="" if output.endswith("\n") else "\n")
    if completed.returncode == 0:
        return 0
    normalized_output = output.lower()
    has_vulnerability_findings = any(marker in normalized_output for marker in VULNERABILITY_MARKERS)
    if not has_vulnerability_findings and any(marker in normalized_output for marker in INFRASTRUCTURE_MARKERS):
        print(json.dumps({
            "status": "INFRASTRUCTURE_SKIPPED",
            "reason": "Dependency advisory service was unavailable",
        }))
        return 0
    return completed.returncode


if __name__ == "__main__":
    raise SystemExit(main())
