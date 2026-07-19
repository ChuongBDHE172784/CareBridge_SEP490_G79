"""Dataset loading and schema export."""

from __future__ import annotations

import json
from pathlib import Path

from .models import BenchmarkDataset


def load_dataset(path: Path) -> BenchmarkDataset:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"Invalid JSON in evaluation dataset {path}: {exc}") from exc
    return BenchmarkDataset.model_validate(payload)


def export_dataset_schema(path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(BenchmarkDataset.model_json_schema(), ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

