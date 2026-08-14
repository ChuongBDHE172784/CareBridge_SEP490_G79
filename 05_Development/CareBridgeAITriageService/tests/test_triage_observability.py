from __future__ import annotations

from pathlib import Path

from app.triage.evidence_retrieval import load_verified_corpus
from app.triage.observability import TriageMetrics


def test_metrics_are_bounded_and_never_retain_health_text_or_ids():
    telemetry = TriageMetrics()
    telemetry.record_turn({
        "triageOutcome": "RED", "targetEntity": "CONFLICTED",
        "plannedQuestionIds": [], "questionRound": 3,
        "latestUserMessage": "PRIVATE HEALTH TEXT", "sessionId": "patient-id",
    }, 72.5)
    telemetry.record_error("EXTRACTION_REJECTED")
    telemetry.record_fallback()
    telemetry.record_error("attacker-controlled-private-label")
    snapshot = telemetry.snapshot()

    assert snapshot["outcomes"] == {"RED": 1}
    assert snapshot["errors"] == {
        "EXTRACTION_REJECTED": 1, "TARGET_CONFLICT": 1, "UNEXPECTED": 1,
    }
    assert snapshot["fallbacks"] == 1
    assert snapshot["maxRoundRoutes"] == 1
    assert "PRIVATE" not in str(snapshot)
    assert "patient-id" not in str(snapshot)


def test_citation_rejection_callback_exposes_only_closed_category(tmp_path: Path):
    (tmp_path / "pending.md").write_text(
        "---\nid: pending\nsourceStatus: PENDING\n---\nbody", encoding="utf-8"
    )
    categories: list[str] = []

    documents = load_verified_corpus(
        corpus_dir=tmp_path, allowed_domains={"who.int"}, target="MOTHER",
        stage="PREGNANCY", on_reject=categories.append,
    )

    assert documents == []
    assert categories == ["CITATION_REJECTED"]
