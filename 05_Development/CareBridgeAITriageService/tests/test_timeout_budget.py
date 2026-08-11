import time
import uuid

import app.triage_v2.api as triage_v2_api

from app.config import (
    GEMINI_SETTINGS,
    PYTHON_SERVICE_TIMEOUT_SECONDS,
    REALTIME_SEARCH_TIMEOUT_SECONDS,
)
from app.graph import run_triage
from app.rules.registry import get_registry
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


def test_v2_extraction_deadline_leaves_time_for_the_deterministic_graph(monkeypatch):
    seen = {}

    class Extractor:
        def extract_triage_v2(self, *, text: str, deadline: float | None = None):
            seen["deadline"] = deadline
            return None

    monkeypatch.setattr(triage_v2_api, "get_gemini_client", lambda: Extractor())
    monkeypatch.setattr(
        triage_v2_api, "retrieve_verified_evidence", lambda *args, **kwargs: []
    )
    request = triage_v2_api.TriageV2TurnRequest.model_validate({
        "sessionId": str(uuid.uuid4()),
        "stateVersion": 0,
        "expectedStateVersion": 0,
        "requestId": f"request_{uuid.uuid4().hex[:16]}",
        "messageId": f"message_{uuid.uuid4().hex[:16]}",
        "latestUserMessage": "Tôi bị đau",
        "selectedTarget": "MOTHER",
        "journeyContext": {"stage": "PREGNANCY", "gestationalWeek": 20},
        "signals": {},
        "measurements": {},
        "expectedRulesetHash": get_registry().ruleset_sha256,
    })
    started = time.monotonic()

    triage_v2_api.execute_turn(request)

    assert seen["deadline"] is not None
    assert 0 < seen["deadline"] - started < PYTHON_SERVICE_TIMEOUT_SECONDS


def test_v2_skips_optional_evidence_after_extraction_budget_is_spent(monkeypatch):
    clock = iter((100.0, 106.1, 106.2))
    retrieved = []

    class Extractor:
        def extract_triage_v2(self, *, text: str, deadline: float | None = None):
            return None

    class YellowGraph:
        def invoke(self, state, config):
            return {
                **state,
                "triageOutcome": "YELLOW",
                "requiredAction": "EARLY_CLINICAL_ASSESSMENT",
                "stopConversation": True,
                "plannedQuestionIds": [],
            }

    monkeypatch.setattr(triage_v2_api, "monotonic", lambda: next(clock))
    monkeypatch.setattr(triage_v2_api, "get_gemini_client", lambda: Extractor())
    monkeypatch.setattr(triage_v2_api, "build_triage_v2_graph", lambda: YellowGraph())
    monkeypatch.setattr(
        triage_v2_api, "retrieve_verified_evidence",
        lambda *args, **kwargs: retrieved.append(True) or [],
    )
    request = triage_v2_api.TriageV2TurnRequest.model_validate({
        "sessionId": str(uuid.uuid4()),
        "stateVersion": 0,
        "expectedStateVersion": 0,
        "requestId": f"request_{uuid.uuid4().hex[:16]}",
        "messageId": f"message_{uuid.uuid4().hex[:16]}",
        "latestUserMessage": "Tôi bị đau",
        "selectedTarget": "MOTHER",
        "journeyContext": {"stage": "PREGNANCY", "gestationalWeek": 20},
        "signals": {},
        "measurements": {},
        "expectedRulesetHash": get_registry().ruleset_sha256,
    })

    response = triage_v2_api.execute_turn(request)

    assert response.state["citations"] == []
    assert retrieved == []
