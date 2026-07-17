from pathlib import Path

import pytest

from carebridge_evaluation.adapters.api import ApiAdapter
from carebridge_evaluation.catalog import load_official_catalog
from carebridge_evaluation.config import EvaluationSettings
from carebridge_evaluation.models import ExecutionStatus
from carebridge_evaluation.preflight import PreflightResult


def test_failed_preflight_marks_api_cases_infrastructure_skipped():
    case = next(case for case in load_official_catalog().cases if "API_END_TO_END" in {mode.value for mode in case.supportedModes})
    adapter = ApiAdapter(EvaluationSettings(api_base_url=None, test_jwt=None, ai_service_path=Path(".")))
    adapter.preflight_result = PreflightResult(False, "Missing CAREBRIDGE_TEST_JWT")

    result = adapter.execute(case, "run-1")

    assert result.executionStatus == ExecutionStatus.INFRASTRUCTURE_SKIPPED
    assert result.passed is None


def test_continue_is_never_marked_retry_safe(monkeypatch):
    adapter = ApiAdapter(EvaluationSettings(
        api_base_url="http://localhost:8080",
        test_jwt="token",
        ai_service_path=Path("."),
    ))
    observed = []

    def fake_request(method, path, *, payload, retry_safe):
        observed.append((method, path, retry_safe))
        raise RuntimeError("stop")

    monkeypatch.setattr(adapter, "_request", fake_request)
    with pytest.raises(RuntimeError, match="stop"):
        adapter._post("/api/v1/triage/intake/conversation/continue", {}, retry_safe=False)

    assert observed == [("POST", "/api/v1/triage/intake/conversation/continue", False)]
