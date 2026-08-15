from __future__ import annotations

from app.triage.evidence_finalizer import finalize_evidence, rationale_for_state


def test_terminal_red_has_deterministic_vietnamese_rationale_and_pending_evidence():
    state = {
        "triageOutcome": "RED",
        "requiredAction": "IMMEDIATE_EMERGENCY_ASSESSMENT",
        "decisiveRuleIds": ["GLOBAL_RED_001"],
        "citations": [],
        "evidenceRejections": [],
    }

    finalized = finalize_evidence(state, retrieval_attempted=False)

    assert finalized["evidenceStatus"] == "PENDING"
    assert "Mất ý thức, co giật hoặc khó thở nghiêm trọng" in finalized["rationale"]
    assert "GLOBAL_RED_001" not in finalized["rationale"]


def test_available_requires_at_least_one_citation_and_mixed_rejections_are_retained():
    state = {
        "triageOutcome": "YELLOW",
        "requiredAction": "EARLY_CLINICAL_ASSESSMENT",
        "decisiveRuleIds": ["PREG_YELLOW_001"],
        "citations": [{"sourceId": "VALID"}],
        "evidenceRejections": ["CITATION_RULE_MISMATCH"],
    }

    finalized = finalize_evidence(state, retrieval_attempted=True)

    assert finalized["evidenceStatus"] == "AVAILABLE"
    assert finalized["citations"] == [{"sourceId": "VALID"}]
    assert finalized["evidenceRejections"] == ["CITATION_RULE_MISMATCH"]


def test_rejected_and_unavailable_are_distinct_and_never_change_outcome():
    rejected = finalize_evidence(
        {
            "triageOutcome": "YELLOW",
            "decisiveRuleIds": [],
            "citations": [],
            "evidenceRejections": ["CITATION_REJECTED"],
        },
        retrieval_attempted=True,
    )
    unavailable = finalize_evidence(
        {"triageOutcome": "YELLOW", "decisiveRuleIds": [], "citations": []},
        retrieval_attempted=True,
    )

    assert rejected["evidenceStatus"] == "REJECTED"
    assert unavailable["evidenceStatus"] == "UNAVAILABLE"
    assert rejected["triageOutcome"] == unavailable["triageOutcome"] == "YELLOW"


def test_rationale_falls_back_safely_for_legacy_state_without_decisive_rules():
    rationale = rationale_for_state({"triageOutcome": "YELLOW"})

    assert rationale == (
        "Kết quả Vàng được xác định từ các dữ kiện đã cung cấp; "
        "bạn nên được nhân viên y tế đánh giá sớm."
    )
