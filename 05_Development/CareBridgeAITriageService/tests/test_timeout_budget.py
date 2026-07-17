import time

from app.config import (
    GEMINI_SETTINGS,
    PYTHON_SERVICE_TIMEOUT_SECONDS,
    REALTIME_SEARCH_TIMEOUT_SECONDS,
)
from app.graph import run_triage
from app.schemas import ChildTriageRequest


def test_normal_deterministic_request_returns_within_seven_second_budget():
    started = time.monotonic()
    response = run_triage(ChildTriageRequest(
        stage="INFANT",
        childAgeMonths=12,
        symptomList=["sot", "ho"],
        temperatureC=38.2,
        breathingStatus="tho binh thuong",
    ), deterministic_only=True)
    elapsed = time.monotonic() - started

    assert response.riskLevel == "NEED_MORE_INFO"
    assert elapsed < PYTHON_SERVICE_TIMEOUT_SECONDS
    assert PYTHON_SERVICE_TIMEOUT_SECONDS == 7.0
    assert GEMINI_SETTINGS.timeout_seconds <= 6.0
    assert REALTIME_SEARCH_TIMEOUT_SECONDS <= 4.0
