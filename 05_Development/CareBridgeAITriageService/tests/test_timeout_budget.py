import time
import uuid

import app.triage.api as triage_api

from app.config import PYTHON_SERVICE_TIMEOUT_SECONDS
from app.rules.registry import get_registry


def test_v2_extraction_deadline_leaves_time_for_the_deterministic_graph(monkeypatch):
    seen = {}

    class Extractor:
        def extract_triage(self, *, text: str, deadline: float | None = None):
            seen["deadline"] = deadline
            return None

    monkeypatch.setattr(triage_api, "get_gemini_client", lambda: Extractor())
    monkeypatch.setattr(
        triage_api, "retrieve_verified_evidence", lambda *args, **kwargs: []
    )
    request = triage_api.TriageTurnRequest.model_validate({
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

    triage_api.execute_turn(request)

    assert seen["deadline"] is not None
    assert 0 < seen["deadline"] - started < PYTHON_SERVICE_TIMEOUT_SECONDS


def test_v2_skips_optional_evidence_after_extraction_budget_is_spent(monkeypatch):
    clock = iter((100.0, 106.1, 106.2))
    retrieved = []

    class Extractor:
        def extract_triage(self, *, text: str, deadline: float | None = None):
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

    monkeypatch.setattr(triage_api, "monotonic", lambda: next(clock))
    monkeypatch.setattr(triage_api, "get_gemini_client", lambda: Extractor())
    monkeypatch.setattr(triage_api, "build_triage_graph", lambda: YellowGraph())
    monkeypatch.setattr(
        triage_api, "retrieve_verified_evidence",
        lambda *args, **kwargs: retrieved.append(True) or [],
    )
    request = triage_api.TriageTurnRequest.model_validate({
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

    response = triage_api.execute_turn(request)

    assert response.state["citations"] == []
    assert retrieved == []
