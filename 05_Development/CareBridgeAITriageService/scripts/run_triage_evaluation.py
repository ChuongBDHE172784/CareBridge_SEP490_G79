"""Compatibility wrapper for the official CareBridge AI Evaluation CLI."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
EVALUATOR_SRC = REPOSITORY_ROOT / "06_Testing" / "CareBridgeAIEvaluation" / "src"
if str(EVALUATOR_SRC) not in sys.path:
    sys.path.insert(0, str(EVALUATOR_SRC))

from carebridge_evaluation.cli import main as evaluation_main


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("local", "api", "all"), default="local")
    parser.add_argument("--output", type=Path, help="Legacy JSON path; its parent becomes the report directory")
    args = parser.parse_args()
    output_dir = args.output.parent if args.output else REPOSITORY_ROOT / "06_Testing" / "CareBridgeAIEvaluation" / "reports" / "latest"
    return evaluation_main(["run", "--mode", args.mode, "--output-dir", str(output_dir)])


if __name__ == "__main__":
    raise SystemExit(main())
