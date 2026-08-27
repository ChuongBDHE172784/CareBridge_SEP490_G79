"""Run the three local Promptfoo suites and conditionally run the API RAG suite."""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess  # nosec B404
from pathlib import Path

from carebridge_evaluation.config import EvaluationSettings
from carebridge_evaluation.preflight import ApiPreflight

LOCAL_CONFIGS = ("pediatric-safety.yaml", "legal-safety.yaml", "prompt-injection.yaml")
RAG_CONFIG = "rag-citation.yaml"
API_FIXTURES = (
    "CAREBRIDGE_API_BASE_URL",
    "CAREBRIDGE_TEST_JWT",
    "CAREBRIDGE_TEST_BABY_PROFILE_ID",
    "CAREBRIDGE_TEST_MOTHER_PROFILE_ID",
)


def promptfoo_status() -> tuple[str, str]:
    if shutil.which("promptfoo") is None:
        return "INFRASTRUCTURE_SKIPPED", "Promptfoo executable is not installed"
    return "READY", "Local deterministic Promptfoo suites are ready"


def rag_status() -> tuple[str, str]:
    missing = [name for name in API_FIXTURES if not (os.getenv(name) or "").strip()]
    if missing:
        return "INFRASTRUCTURE_SKIPPED", f"Missing non-production fixtures: {', '.join(missing)}"
    preflight = ApiPreflight(EvaluationSettings.from_env()).run()
    if not preflight.ready:
        return "INFRASTRUCTURE_SKIPPED", f"RAG/citation API preflight failed: {preflight.reason}"
    return "READY", "RAG/citation Promptfoo suite passed API, fixture, and schema preflight"


def _command(executable: str, config: Path, output: Path) -> list[str]:
    args = [executable, "eval", "-c", str(config), "-o", str(output)]
    if os.name == "nt" and executable.lower().endswith((".cmd", ".bat")):
        return ["cmd", "/c", *args]
    return args


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=Path("reports/promptfoo"))
    args = parser.parse_args()
    ready, reason = promptfoo_status()
    if ready != "READY":
        print(json.dumps({"status": ready, "reason": reason}))
        return 1

    root = Path(__file__).resolve().parents[1]
    promptfoo_dir = root / "promptfoo"
    args.output_dir = args.output_dir.resolve()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    executable = shutil.which("promptfoo")
    if executable is None:
        raise RuntimeError("Promptfoo became unavailable after readiness check")

    statuses: dict[str, str] = {}
    for config in LOCAL_CONFIGS:
        output = args.output_dir / f"{Path(config).stem}.json"
        completed = subprocess.run(  # nosec B603
            _command(executable, promptfoo_dir / config, output),
            cwd=promptfoo_dir,
            check=False,
        )
        if completed.returncode != 0:
            return completed.returncode
        statuses[config] = "COMPLETED"

    rag_ready, rag_reason = rag_status()
    if rag_ready == "READY":
        completed = subprocess.run(  # nosec B603
            _command(executable, promptfoo_dir / RAG_CONFIG, args.output_dir / "rag-citation.json"),
            cwd=promptfoo_dir,
            check=False,
        )
        if completed.returncode != 0:
            return completed.returncode
        statuses[RAG_CONFIG] = "COMPLETED"
    else:
        statuses[RAG_CONFIG] = rag_ready

    overall_status = "COMPLETED" if rag_ready == "READY" else "PARTIAL"
    print(json.dumps({"status": overall_status, "configs": statuses, "ragReason": rag_reason if rag_ready != "READY" else None}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
