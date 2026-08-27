from pathlib import Path

import pytest

from carebridge_evaluation.config import EvaluationSettings


def test_runtime_timeouts_default_above_spring_timeout(monkeypatch, tmp_path: Path):
    for name in (
        "EVALUATION_REQUEST_TIMEOUT_SECONDS",
        "EVALUATION_CASE_BUDGET_SECONDS",
        "EVALUATION_MAX_RETRIES",
    ):
        monkeypatch.delenv(name, raising=False)

    settings = EvaluationSettings.from_env(repository_root=tmp_path)

    assert settings.request_timeout_seconds == 20
    assert settings.request_timeout_seconds > 15
    assert settings.case_budget_seconds == 90
    assert settings.max_retries == 1


def test_non_positive_timeout_is_rejected(monkeypatch, tmp_path: Path):
    monkeypatch.setenv("EVALUATION_REQUEST_TIMEOUT_SECONDS", "0")
    with pytest.raises(ValueError, match="greater than zero"):
        EvaluationSettings.from_env(repository_root=tmp_path)

