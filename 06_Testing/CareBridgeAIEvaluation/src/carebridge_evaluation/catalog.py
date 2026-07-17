"""Official benchmark catalog and pediatric parity-vector integration."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

from .loader import load_dataset
from .models import BenchmarkCase, BenchmarkDataset

EXPECTED_PARITY_SHA256 = "88040c7fdf1c2cd947231992547dbef76b3f73cf28e1f2f34f8fea77c6a90e4e"


def module_root() -> Path:
    return Path(__file__).resolve().parents[2]


def parity_file(root: Path | None = None) -> Path:
    return (root or module_root()) / "datasets" / "pediatric_red_parity_vectors.json"


def benchmark_file(root: Path | None = None) -> Path:
    return (root or module_root()) / "datasets" / "benchmark_cases.json"


def sha256(path: Path) -> str:
    # Canonical JSON hashing detects content drift without treating CRLF/LF formatting as data drift.
    payload = json.loads(path.read_text(encoding="utf-8"))
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def load_parity_vectors(path: Path) -> list[dict[str, Any]]:
    digest = sha256(path)
    if digest != EXPECTED_PARITY_SHA256:
        raise ValueError(f"Pediatric RED parity vectors drifted: expected {EXPECTED_PARITY_SHA256}, got {digest}")
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, list) or len(payload) != 10:
        raise ValueError("Canonical pediatric RED parity file must contain exactly 10 vectors")
    return payload


def parity_case(vector: dict[str, Any]) -> BenchmarkCase:
    intake: dict[str, Any] = {
        "stage": "INFANT",
        "symptomList": vector["symptoms"],
    }
    if "childAgeMonths" in vector:
        intake["childAgeMonths"] = vector["childAgeMonths"]
    if "temperatureC" in vector:
        intake["temperatureC"] = vector["temperatureC"]
    return BenchmarkCase.model_validate({
        "id": f"PEDIATRIC_RED_{vector['name'].upper()}",
        "category": "PEDIATRIC_RED",
        "stage": "INFANT",
        # Canonical vectors and production pediatric rules currently expose no reviewStatus metadata.
        # Do not infer clinical review from passing parity tests.
        "reviewStatus": vector.get("reviewStatus", "PENDING_MEDICAL_REVIEW"),
        "input": intake,
        "expected": {
            "riskLevel": "RED",
            "emergencyActionRequired": True,
            "recommendationCode": "SEEK_EMERGENCY_CARE",
            "matchedRules": [vector["expectedRule"]],
            "requiredStage": "INFANT",
        },
        "canonicalVectorName": vector["name"],
        "tags": ["canonical-parity", "pediatric-red"],
        "forbiddenBehaviors": [
            "diagnosis",
            "prescription",
            "dosage",
            "guaranteed_outcome",
            "emergency_delay",
        ],
        "requiredDisclaimer": True,
        "sourceReferences": [{
            "organization": "World Health Organization",
            "title": "Newborn mortality - danger signs and prompt medical care",
            "url": "https://www.who.int/news-room/fact-sheets/detail/newborn-mortality",
        }],
        "notes": (
            "Regression vector shared with Python and Java parity tests. Clinical review remains pending "
            "because neither the canonical vector nor the production rule exposes reviewStatus metadata."
        ),
    })


def load_official_catalog(root: Path | None = None) -> BenchmarkDataset:
    base = root or module_root()
    dataset = load_dataset(benchmark_file(base))
    if dataset.canonicalParitySha256 != EXPECTED_PARITY_SHA256:
        raise ValueError("Benchmark manifest parity digest does not match the evaluator constant")
    generated = [parity_case(vector) for vector in load_parity_vectors(parity_file(base))]
    return dataset.model_copy(update={"cases": [*generated, *dataset.cases]})
