"""Bounded, process-local V2 telemetry with no health text or identifiers."""

from __future__ import annotations

from collections import Counter
from threading import Lock
from typing import Mapping


_OUTCOMES = {"RED", "YELLOW", "NEEDS_MORE_INFO", "OUT_OF_SCOPE", "NONE"}
_ERRORS = {
    "HASH_MISMATCH", "INVALID_STATE", "EXTRACTION_REJECTED", "CITATION_REJECTED",
    "TARGET_CONFLICT", "UNEXPECTED",
}
_LATENCY_BUCKETS_MS = (50, 100, 250, 500, 1_000, 2_500, 5_000)


class TriageV2Metrics:
    """Low-cardinality counters only; labels are closed enums, never caller data."""

    def __init__(self) -> None:
        self._lock = Lock()
        self._outcomes: Counter[str] = Counter()
        self._errors: Counter[str] = Counter()
        self._latency: Counter[str] = Counter()
        self._turns = 0
        self._latency_total_ms = 0
        self._questions_total = 0
        self._max_round_routes = 0
        self._fallbacks = 0

    def record_turn(self, state: Mapping[str, object], latency_ms: float) -> None:
        outcome = str(_value(state.get("triageOutcome")) or "NONE")
        if outcome not in _OUTCOMES:
            outcome = "NONE"
        elapsed = max(0, min(int(latency_ms), 60_000))
        questions = state.get("plannedQuestionIds")
        question_count = len(questions) if isinstance(questions, list) else 0
        with self._lock:
            self._turns += 1
            self._outcomes[outcome] += 1
            self._latency_total_ms += elapsed
            self._questions_total += min(question_count, 3)
            if state.get("questionRound") == 3:
                self._max_round_routes += 1
            if (str(_value(state.get("contextResolutionStatus"))) == "CONFLICTED"
                    or str(_value(state.get("targetEntity"))) == "CONFLICTED"):
                self._errors["TARGET_CONFLICT"] += 1
            bucket = next((limit for limit in _LATENCY_BUCKETS_MS if elapsed <= limit), None)
            self._latency[str(bucket) if bucket is not None else "+Inf"] += 1

    def record_error(self, category: str) -> None:
        safe = category if category in _ERRORS else "UNEXPECTED"
        with self._lock:
            self._errors[safe] += 1

    def record_fallback(self) -> None:
        with self._lock:
            self._fallbacks += 1

    def snapshot(self) -> dict[str, object]:
        with self._lock:
            turns = self._turns
            return {
                "turns": turns,
                "outcomes": dict(sorted(self._outcomes.items())),
                "errors": dict(sorted(self._errors.items())),
                "fallbacks": self._fallbacks,
                "latencyBucketsMs": dict(sorted(self._latency.items())),
                "averageLatencyMs": round(self._latency_total_ms / turns, 2) if turns else 0,
                "averageQuestionCount": round(self._questions_total / turns, 2) if turns else 0,
                "maxRoundRoutes": self._max_round_routes,
            }


metrics = TriageV2Metrics()


def _value(value: object) -> object:
    return getattr(value, "value", value)
