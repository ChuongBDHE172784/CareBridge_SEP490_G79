from __future__ import annotations

import json

import pytest
from pydantic import ValidationError

from carebridge_evaluation.loader import load_dataset
from carebridge_evaluation.models import (
    BenchmarkCategory,
    ExecutionMode,
    ExpectedExecutionStatus,
    MetricResult,
    MetricStatus,
    ReviewStatus,
)


def _case(case_id: str = "CASE_001") -> dict:
    return {
        "id": case_id,
        "category": "PEDIATRIC_RED",
        "stage": "INFANT",
        "reviewStatus": "CONFIRMED_REVIEWED",
        "supportedModes": ["LOCAL_DETERMINISTIC", "API_END_TO_END"],
        "input": {"stage": "INFANT", "symptomList": ["seizure"]},
        "expected": {"riskLevel": "RED", "emergencyActionRequired": True},
        "forbiddenBehaviors": ["diagnosis"],
        "requiredDisclaimer": True,
        "sourceReferences": [{
            "organization": "WHO",
            "title": "Newborn mortality",
            "url": "https://www.who.int/news-room/fact-sheets/detail/newborn-mortality",
        }],
        "notes": "Test fixture",
    }


def test_dataset_loader_accepts_explicit_stage_and_stable_metadata(tmp_path):
    path = tmp_path / "cases.json"
    path.write_text(
        json.dumps({"schemaVersion": "2.0", "name": "test", "cases": [_case()]}),
        encoding="utf-8",
    )

    dataset = load_dataset(path)

    assert dataset.cases[0].category == BenchmarkCategory.PEDIATRIC_RED
    assert dataset.cases[0].reviewStatus == ReviewStatus.CONFIRMED_REVIEWED
    assert dataset.cases[0].supportedModes == [ExecutionMode.LOCAL_DETERMINISTIC, ExecutionMode.API_END_TO_END]


def test_dataset_rejects_implicit_stage(tmp_path):
    case = _case()
    case["input"].pop("stage")
    path = tmp_path / "cases.json"
    path.write_text(
        json.dumps({"schemaVersion": "2.0", "name": "test", "cases": [case]}),
        encoding="utf-8",
    )

    with pytest.raises(ValidationError, match="send the stage explicitly"):
        load_dataset(path)


def test_postpartum_requires_known_scope_gap(tmp_path):
    case = _case()
    case.update({
        "id": "POSTPARTUM_001",
        "category": "POSTPARTUM_MOTHER",
        "stage": None,
        "input": {"parentFreeText": "postpartum context"},
        "reviewStatus": "PENDING_MEDICAL_REVIEW",
        "expectedExecutionStatus": "EXECUTE",
    })
    path = tmp_path / "cases.json"
    path.write_text(
        json.dumps({"schemaVersion": "2.0", "name": "test", "cases": [case]}),
        encoding="utf-8",
    )

    with pytest.raises(ValidationError, match="known or unsupported scope gap"):
        load_dataset(path)


def test_scope_gap_and_review_status_are_independent():
    assert ExpectedExecutionStatus.KNOWN_SCOPE_GAP != ReviewStatus.PENDING_MEDICAL_REVIEW


def test_zero_denominator_metric_cannot_claim_zero_or_one_hundred_percent():
    common = {
        "metricId": "redRecall",
        "displayName": "RED recall",
        "description": "Confirmed RED recall",
        "numerator": 0,
        "denominator": 0,
        "formula": "TP / (TP + FN)",
        "exclusions": [],
        "target": 1.0,
        "targetComparator": ">=",
    }
    with pytest.raises(ValidationError, match="Zero-denominator"):
        MetricResult(**common, value=0.0, status=MetricStatus.FAILED)

    result = MetricResult(
        **common,
        value=None,
        status=MetricStatus.NOT_EVALUATED,
        reason="No confirmed RED cases were executed.",
    )
    assert result.value is None
