import time

from app import graph as graph_module
from app.config import MIN_REALTIME_SEARCH_REMAINING_SECONDS
from app.graph import attach_evidence, realtime_official_search_if_needed
from app.schemas import ChildTriageRequest


def test_realtime_search_is_skipped_when_gemini_leaves_less_than_one_second(monkeypatch):
    calls = {"count": 0}

    def should_not_run(*_args, **_kwargs):
        calls["count"] += 1
        return []

    monkeypatch.setattr(graph_module, "retrieve_realtime_sources", should_not_run)
    state = {
        "intake": ChildTriageRequest(stage="INFANT", childAgeMonths=12, symptomList=["sot"]),
        "riskLevel": "YELLOW",
        "normalizedSymptoms": ["fever"],
        "matchedRules": ["YELLOW_FEVER_MONITOR"],
        "retrievedSources": [],
        # Simulates Gemini normalization consuming nearly the entire 7s request budget.
        "requestDeadline": time.monotonic() + MIN_REALTIME_SEARCH_REMAINING_SECONDS - 0.01,
    }

    state = realtime_official_search_if_needed(state)
    state = attach_evidence(state)

    assert calls["count"] == 0
    assert state["riskLevel"] == "YELLOW"
    assert state["citations"] == []
    assert state["realtimeSearchSkippedForDeadline"] is True
    assert "KhÃ´ng Ä‘á»§ thá»i gian" in state["warning"]
